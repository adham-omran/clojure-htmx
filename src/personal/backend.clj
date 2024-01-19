(ns personal.backend
  (:require
   [clojure.java.io :as io]
   [hiccup.page]
   [reitit.dev.pretty :as pretty]
   [reitit.ring.malli]
   [reitit.ring.middleware.muuntaja :as muuntaja]
   [reitit.ring.middleware.exception :as exception]
   [reitit.coercion.malli]
   [muuntaja.core :as m]
   [reitit.ring :as ring]
   [reitit.ring.coercion :as coercion]
   [reitit.ring.middleware.multipart :as multipart]
   [reitit.ring.middleware.parameters :as parameters]
   [ring.adapter.jetty :as adapter]))

(def some-data {:x 1
                :y 2
                :z 3})

(defn handler [_]
  {:status 200
   :body (hiccup.page/html5 [:p. (str some-data)])})

(defn wrap [handler id]
  (fn [request]
    (update (handler request) :wrap (fnil conj '()) id)))

(def app
  (ring/ring-handler
   (ring/router
    [["/" (constantly {:status 200
                       :body (slurp (io/resource "./public/index.html"))})]
     ["/trigger" {:get {#_:headers #_{"Content-Type" "text/html"}
                        :coercion reitit.coercion.malli/coercion
                        :handler (fn [req]
                                   (println "hello")
                                   (println {:request req})
                                   {:body (hiccup.page/html5 [:div "RESULTS"])})
                        :parameters {:query [:map [:q string?]]}}}]
     ["/api/ping" {:post {:handler handler}}]
     ["/plus"
      {:get {:summary "plus with malli query parameters"
             :coercion reitit.coercion.malli/coercion
             :parameters {:query [:map
                                  [:y int?]]}
             :responses {200 {:body [:map [:total int?]]}}
             :handler (fn [req]
                        (println req)
                        {:status 200
                         :body {:total (:query-params req)}})}}]
     ["/*" (ring/create-resource-handler)]]

    {:exception pretty/exception
     :muuntaja m/instance
     :conflicts (constantly nil)
     :middleware [;; query-params & form-params
                  parameters/parameters-middleware
                           ;; content-negotiation
                  muuntaja/format-negotiate-middleware
                           ;; encoding response body
                  muuntaja/format-response-middleware
                           ;; exception handling
                  exception/exception-middleware
                           ;; decoding request body
                  muuntaja/format-request-middleware
                           ;; coercing response bodys
                  coercion/coerce-response-middleware
                           ;; coercing request parameters
                  coercion/coerce-request-middleware
                           ;; multipart
                  multipart/multipart-middleware]})
   (ring/routes (ring/create-default-handler))))

(comment
  (app {:request-method :get
        :uri "/trigger"
        :query-params {:q "foo"}})
  (def server
    (adapter/run-jetty #'app {:port 8090
                              :join? false}))
  (.stop server))
