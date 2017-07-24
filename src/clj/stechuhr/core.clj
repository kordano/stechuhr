( ns stechuhr.core
  (:require [hasch.core :refer [uuid]]
            [replikativ.peer :refer [server-peer]]

            [kabel.peer :refer [start stop]]
            [konserve.memory :refer [new-mem-store]]

            [superv.async :refer [<?? S]] ;; core.async error handling
            [clojure.core.async :refer [chan] :as async]

            [taoensso.timbre :as timbre :refer [info log]]
            [taoensso.timbre.appenders.3rd-party.logstash :as logstash]

            [cheshire.core :refer [generate-string]]

            [org.httpkit.server :refer [run-server]]
            [compojure.route :refer [resources not-found]]
            [compojure.core :refer [defroutes]]))

(def uri "ws://127.0.0.1:31778")

(def timbre-config
  {:middleware [(fn [data]
                  (update-in data [:vargs] (fn [vargs] (mapv generate-string vargs))))]
   :appenders {:logstash (logstash/logstash-appender "localhost" 5000)}})

(defroutes base-routes
  (resources "/")
  (not-found "<h1>404. Page not found.</h1>"))

(defn start-server []
  (let [port  8080
        uri   "ws://127.0.0.1:31778"
        store (<?? S (new-mem-store))
        peer  (<?? S (server-peer S store uri))]
    (timbre/merge-config! timbre-config)
    (run-server #'base-routes {:port port})
    (info {:event :http-start :data {:port port}})
    (<?? S (start peer))
    (info {:event :peer-start :data {:uri uri}})
    (<?? S (chan))))

(defn -main [& args]
  (start-server))

(comment

  
  (start-server)

  )
