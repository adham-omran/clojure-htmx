(ns personal.backend
  (:require [reitit.ring :as ring]
            [ring.adapter.jetty :as adapter]
            ))

(defn handler [_]
  {:status 200, :body "ok"})

(defn wrap [handler id]
  (fn [request]
    (update (handler request) :wrap (fnil conj '()) id)))

(comment
  (ring/ring-handler
   (ring/router
    ["/"
     ["home" {:get ""}]
     ["api" {:middleware [[wrap :api]]}
      ["/ping" {:get handler
                :name ::ping}]
      ["/admin" {:middleware [[wrap :admin]]}
       ["/users" {:get handler
                  :post handler}]]]
     ["assets/*" (ring/create-resource-handler)]]
    )
   (ring/create-default-handler)))


(def app
  (ring/ring-handler
   (ring/router
    ["/"
     ["home" {:get
              {:handler (fn [_] {:body (slurp "resources/assets/index.html")
                                 :headers {"Content-Type" "text/html"}})}}]
     ["clicked" {:post
                 {:handler (fn [_] {:body (slurp "resources/assets/response.html")
                                 :headers {"Content-Type" "text/html"}})}}]
     ["api" {:middleware [[wrap :api]]}
      ["/ping" {:get handler
                :name ::ping}]
      ["/admin" {:middleware [[wrap :admin]]}
       ["/users" {:get handler
                  :post handler}]]]
     ["assets/*" (ring/create-resource-handler)]]
    )
   (ring/create-default-handler))
  )


(def server
  (adapter/run-jetty #'app {:port 8090
                            :join? false}))
