(ns stechuhr.core
  (:require [konserve.memory :refer [new-mem-store]]
            [replikativ.peer :refer [client-peer]]
            [replikativ.stage :refer [create-stage! connect! subscribe-crdts!]]
            [hasch.core :refer [uuid]]
            [replikativ.crdt.ormap.realize :refer [stream-into-identity!]]
            [replikativ.crdt.ormap.stage :as s]
            [cljs.core.async :refer [>! chan timeout]]
            [superv.async :refer [S] :as sasync]
            [om.next :as om :refer-macros [defui] :include-macros true]
            [om.dom :as dom :include-macros true]
            [sablono.core :as html :refer-macros [html]])
  (:require-macros [superv.async :refer [go-try <? go-loop-try]]
                   [cljs.core.async.macros :refer [go-loop]]))

(def user "mail:alice@stechuhr.de")
(def ormap-id #uuid "8c518048-1a31-4f0a-bd71-f49ef898db10")
(def uri "ws://127.0.0.1:31778")

(enable-console-print!)

(defonce val-atom (atom {:captures #{}}))

(def stream-eval-fns
  {'assoc (fn [a new]
            (swap! a update-in [:captures] conj new)
            a)
   'dissoc (fn [a new]
             (swap! a update-in [:captures] (fn [old] (set (remove #{new} old))))
             a)})

(defn setup-replikativ []
  (go-try S
          (let [store (<? S (new-mem-store))
                peer (<? S (client-peer S store))
                stage (<? S (create-stage! user peer))
                stream (stream-into-identity! stage [user ormap-id] stream-eval-fns val-atom)]
            (<? S (s/create-ormap! stage :description "captures" :id ormap-id))
            (connect! stage uri)
            {:store store
             :stage stage
             :stream stream
             :peer peer})))

(declare client-state)

(defn add-capture! [app-state capture]
  (s/assoc! (:stage app-state)
            [user ormap-id]
            (uuid capture)
            [['assoc capture]]))

(defn input-widget [component placeholder local-key]
  [:input {:value (get (om/get-state component) local-key)
           :placeholder placeholder
           :on-change (fn [e] (om/update-state! component assoc local-key (.. e -target -value)))}])

(defui App
  Object
  (componentWillMount
   [this]
   (om/set-state! this {:input-project ""
                        :input-task ""
                        :input-capture ""}))
  (render [this]
    (let [{:keys [input-project input-task input-capture]} (om/get-state this)
          {:keys [captures]} (om/props this)]
      (html
       [:div
        [:div.widget
         [:h1 "Time Captures"]
         (input-widget this "Project" :input-project)
         (input-widget this "Task" :input-task)
         (input-widget this "Capture" :input-capture)
         [:button
          {:on-click (fn [e] (let [new-capture
                                   {:project input-project
                                    :task input-task
                                    :capture input-capture}]
                               (do
                                 (add-capture! client-state new-capture)
                                 (om/update-state! this assoc :input-project "")
                                 (om/update-state! this assoc :input-task "")
                                 (om/update-state! this assoc :input-capture ""))) )}
          "Add"]]
        [:div.widget
         [:table
          [:tr
           [:th "Project"]
           [:th "Task"]
           [:th "Capture"]]
          (mapv
           (fn [{:keys [project task capture]}]
             [:tr
              [:td project]
              [:td task]
              [:td capture]])
           captures)]]]))))

(defn main [& args]
  (go-try S (def client-state (<? S (setup-replikativ))))
  (.error js/console "Stechuhr started ..."))

(def reconciler
  (om/reconciler {:state val-atom}))

(om/add-root! reconciler App (.getElementById js/document "app"))

(comment

  (->> val-atom
       deref
       :captures)

  )
