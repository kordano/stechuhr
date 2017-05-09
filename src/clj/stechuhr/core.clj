(ns stechuhr.core
  (:require [hasch.core :refer [uuid]]
            [replikativ.peer :refer [server-peer]]

            [kabel.peer :refer [start stop]]
            [konserve.memory :refer [new-mem-store]]

            [superv.async :refer [<?? S]] ;; core.async error handling
            [clojure.core.async :refer [chan] :as async]

            [org.httpkit.server :refer [run-server]]
            [compojure.route :refer [resources not-found]]
            [compojure.core :refer [defroutes]]))

(def uri "ws://127.0.0.1:31778")

(defroutes base-routes
  (resources "/")
  (not-found "<h1>404. Page not found.</h1>"))

(defn start-server []
  (let [store (<?? S (new-mem-store))
        peer (<?? S (server-peer S store uri))]
    (run-server #'base-routes {:port 8080})
    (println "http server started!")
    (<?? S (start peer))
    (println "replikativ peer startet!")
    (<?? S (chan))))

(defn -main [& args]
  (start-server))
