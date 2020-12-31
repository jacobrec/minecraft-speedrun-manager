package main

import (
	"fmt"
	"net/http"
)

type Speedrun struct {
	StartTime int      `json:"starttime"`
	Completed int      `json:"completed"`
	Address   string   `json:"address"`
	Id        int      `json:"id"`
	People    []string `json:"people"`
}

func hello(w http.ResponseWriter, req *http.Request) {
	fmt.Fprintf(w, "hello\n")
}

func main() {

	http.HandleFunc("/client/data", hello)
	http.ListenAndServe(":8000", nil)
}
