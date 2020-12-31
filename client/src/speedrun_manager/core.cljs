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
(def data {:servers [{:address "speedrun" :starttime false :completed false :id 3234
                      :people ["jacobreckhard" "petelliott332"]}
                     {:address "speedrun1" :starttime 1609363090 :completed false :id 2305
                      :people ["jacobreckhard" "petelliott332"]}
                     {:address "speedrun" :starttime 1609463090 :completed 1609473090 :id 5644
                      :people ["jacobreckhard" "petelliott332"]}]})

(defn update-data []
  (go (let [response (<! (http/get (str httpserver-base-address "client/data")))])
    (prn (:status response))
    (prn (:body response))))


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
    (doall (for [x players] [:p x]))])

(defn server-display [nowtime address starttime completed players]
  (def mtime (if starttime (- (or completed (quot @nowtime 1000)) starttime) 0))
  [:li.hor.card.serveritem
   [:p address + server-base-address]
   [clock-component mtime]
   (if players (show-players players) [:button.outline.delete "-"])])

(defn add-button []
  [:button.outline.add "+"])

(defn nav-bar [page]
  [:div.card.marg-y.nav
   (cond
     (= :home @page) [:a {:href "#" :on-click #(reset! page :score)} "Leaderboards"]
     (= :score @page) [:a {:href "#" :on-click #(reset! page :home)} "Home"])])


(defn servers-display-current [time data]
  (doall (for [x (data :servers) :when (not (x :completed))]
          [server-display time (x :address) (x :starttime) (x :completed) nil])))
(defn servers-display-past [time data]
  (doall (for [x (sort-by #(- (% :completed) (% :starttime)) (data :servers))
               :when (x :completed)]
          [server-display time (x :address) (x :starttime) (x :completed) (x :people)])))

(let [time (r/atom (.now js/Date))
      page (r/atom :home)]
  (js/setInterval (fn [] (print @time) (reset! time (.now js/Date))) 1000)
  (print (data :servers))
  (defn home-page []
      (fn []
        [:div.ver.cent [:h1 "Minecraft Speedrun Manager"]
         (nav-bar page)
         [:ul.marg-x.serverlist
          (servers-display-current time data)]
         (add-button)]))

  (defn score-page []
    (fn []
      [:div.ver.cent [:h1 "Minecraft Speedrun History"]
       (nav-bar page)
       [:ul.marg-x.serverlist
        (servers-display-past time data)]]))

  (defn main-page []
    [:div.all.dark
      (cond
        (= @page :home) [home-page]
        (= @page :score) [score-page])]))


;; -------------------------
;; Initialize app

(defn mount-root []
  (d/render [main-page] (.getElementById js/document "app")))

(defn ^:export init! []
  (mount-root))
