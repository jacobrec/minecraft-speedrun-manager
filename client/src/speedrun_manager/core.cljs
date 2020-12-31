(ns speedrun-manager.core
    (:require
      [reagent.core :as r]
      [reagent.dom :as d]
      [cljs-http.client :as http])
    (:require-macros [cljs.core.async.macros :refer [go]]))

;; -------------------------
;; Views
(def server-base-address ".woosaree.xyz")
(def httpserver-base-address "http://localhost:8000/")

(def callback nil)


(defn doublezero [num]
  (if (< num 10) (str "0" num) (str num)))
(defn format-elapsed-time [time]
  (def seconds (doublezero (mod time 60)))
  (def minutes (doublezero (mod (quot time 60) 60)))
  (def hours (doublezero (quot time 3600)))
  (str hours ":" minutes ":" seconds))

(defn clock-component [mtime]
  [:div.hor
   [:p.pad-x "Elapsed: "]
   [:p (format-elapsed-time mtime)]])

(defn show-players [players]
  [:div
    (doall (for [x players] [:p {:key x} x]))])

(defn delete-server [id]
  (go (let [response (<! (http/post
                          (str httpserver-base-address "client/deleteserver")
                          {:with-credentials? false :form-params {:id id}}))])
    (callback)))

(defn new-server [address]
  (go (let [response (<! (http/post
                          (str httpserver-base-address "client/newserver")
                          {:with-credentials? false :form-params {:address address}}))])
    (callback)))

(defn server-display [nowtime address starttime completed players id]
  (def mtime (if (= 0 starttime) 0
               (- (if (= 0 completed) (quot @nowtime 1000) completed) starttime)))
  [:li.hor.card.serveritem {:key id}
   [:p address + server-base-address]
   [clock-component mtime]
   (if players (show-players players) [:button.outline.delete {:on-click #(delete-server id)} "-"])])


(defn add-button []
  [:button.outline.add {:on-click #(new-server "speedrun")} "+"])

(defn nav-bar [page]
  [:div.card.marg-y.nav
   (cond
     (= :home @page) [:a {:href "#" :on-click #(reset! page :score)} "Leaderboards"]
     (= :score @page) [:a {:href "#" :on-click #(reset! page :home)} "Home"])])


(defn servers-display-current [time data]
  (doall (for [x (data :servers) :when (= 0 (x :completed))]
          [server-display time (x :address) (x :starttime) (x :completed) nil (x :id)])))
(defn servers-display-past [time data]
  (doall (for [x (sort-by #(- (% :completed) (% :starttime)) (data :servers))
               :when (not (= 0 (x :completed)))]
          [server-display time (x :address) (x :starttime) (x :completed) (x :people) (x :id)])))

(let [time (r/atom (.now js/Date))
      page (r/atom :home)
      data (r/atom {:servers []})]

  (js/setInterval (fn [] (print @time) (reset! time (.now js/Date))) 1000)
  (defn update-data []
    (go (let [response (<! (http/get
                            (str httpserver-base-address "client/data")
                            {:with-credentials? false}))]
          (print (:body response))
          (reset! data (:body response)))))

  (js/setInterval (fn [] (update-data)) 5000)
  (update-data)
  (set! callback update-data)

  (defn home-page [data]
      (fn []
        [:div.ver.cent [:h1 "Minecraft Speedrun Manager"]
         (nav-bar page)
         [:ul.marg-x.serverlist
          (servers-display-current time @data)]
         (add-button)]))

  (defn score-page [data]
    (fn []
      [:div.ver.cent [:h1 "Minecraft Speedrun History"]
       (nav-bar page)
       [:ul.marg-x.serverlist
        (servers-display-past time @data)]]))

  (defn main-page []
    [:div.all.dark
      (cond
        (= @page :home) [home-page data]
        (= @page :score) [score-page data])]))


;; -------------------------
;; Initialize app

(defn mount-root []
  (d/render [main-page] (.getElementById js/document "app")))

(defn ^:export init! []
  (mount-root))
