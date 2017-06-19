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
            [taoensso.timbre :as timbre]
            [sablono.core :as html :refer-macros [html]])
  (:require-macros [superv.async :refer [go-try <? go-loop-try]]
                   [cljs.core.async.macros :refer [go-loop]]))

(enable-console-print!)

(def user "mail:alice@stechuhr.de")

(def ormap-id #uuid "07f6aae2-2b46-4e44-bfd8-058d13977a8a")

(def uri "ws://127.0.0.1:31778")

(defonce val-atom (atom {:captures #{}}))

(def stream-eval-fns
  {'add    (fn [S a new]
             (swap! a update-in [:captures] conj new)
             a)
   'remove (fn [S a new]
             (swap! a update-in [:captures] (fn [old] (set (remove #{new} old))))
             a)})

(defn setup-replikativ []
  (go-try
   S
   (let [store  (<? S (new-mem-store))
         peer   (<? S (client-peer S store))
         stage  (<? S (create-stage! user peer))
         stream (stream-into-identity! stage [user ormap-id] stream-eval-fns val-atom)]
     (<? S (s/create-ormap! stage :description "captures" :id ormap-id))
     (connect! stage uri)
     {:store  store
      :stage  stage
      :stream stream
      :peer   peer})))

(declare replikativ-state)

(defn add-capture! [state capture]
  (s/assoc! (:stage state)
            [user ormap-id]
            (uuid capture)
            [['add capture]]))

(defn input-widget [component placeholder local-key]
  [:input {:value       (get (om/get-state component) local-key)
           :placeholder placeholder
           :on-change   (fn [e]
                          (om/update-state!
                           component
                           assoc
                           local-key
                           (.. e -target -value)))}])

(defui App
  Object
  (componentWillMount
    [this]
    (om/set-state! this {:input-project ""
                         :input-task    ""
                         :input-capture ""}))
  (render [this]
    (let [{:keys [input-project input-task input-capture]} (om/get-state this)
          {:keys [captures]}                               (om/props this)]
      (html
       [:div
        [:div.widget
         [:h1 "Task Captures"]
         (input-widget this "Project" :input-project)
         (input-widget this "Task" :input-task)
         (input-widget this "Capture" :input-capture)
         [:button
          {:on-click (fn [_]
                       (let [new-capture {:project input-project
                                          :task    input-task
                                          :capture input-capture}]
                         (do
                           (add-capture! replikativ-state new-capture)
                           (om/update-state! this assoc :input-project "")
                           (om/update-state! this assoc :input-task "")
                           (om/update-state! this assoc :input-capture ""))))}
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
  (go-try S (def replikativ-state (<? S (setup-replikativ))))
  (.error js/console "Stechuhr connected ...."))

(def reconciler
  (om/reconciler {:state val-atom}))

(om/add-root! reconciler App (.getElementById js/document "app"))


(comment

  (remove #{1} #{1 2 3})

  (-> val-atom
      deref
      :captures)

  (.log js/console (clj->js stream-eval-fns))

  )
