package main

import (
	"encoding/json"
	"fmt"
	"io/ioutil"
	"net/http"
	"os"
	"strconv"
)

type Speedrun struct {
	StartTime int      `json:"starttime"`
	Completed int      `json:"completed"`
	Address   string   `json:"address"`
	Id        int      `json:"id"`
	People    []string `json:"people"`
}

type ServerResponse struct {
	Speedruns []Speedrun `json:"servers"`
}

func readDataFromFile() (*ServerResponse, error) {
	jsonFile, err := os.Open("data.json")
	if err != nil {
		fmt.Fprintf(os.Stderr, "Could not open data file\n")
		return nil, err
	}
	defer jsonFile.Close()
	byteValue, err := ioutil.ReadAll(jsonFile)
	if err != nil {
		fmt.Fprintf(os.Stderr, "Could not read data file\n")
		return nil, err
	}

	var data ServerResponse
	err = json.Unmarshal(byteValue, &data)

	if err != nil {
		fmt.Fprintf(os.Stderr, "Could not parse json in data file\n")
		return nil, err
	}
	return &data, nil
}

var global_data ServerResponse

func cors(w *http.ResponseWriter, req *http.Request) {
	(*w).Header().Set("Access-Control-Allow-Origin", "*")
	(*w).Header().Set("Access-Control-Allow-Methods", "POST, GET, OPTIONS, PUT, DELETE")
	(*w).Header().Set("Access-Control-Allow-Headers", "Accept, Content-Type, Content-Length, Accept-Encoding, X-CSRF-Token, Authorization")
}

func getdata(w http.ResponseWriter, req *http.Request) {

	cors(&w, req)
	if req.Method == "OPTIONS" {
		return
	}
	w.Header().Set("Content-Type", "application/json")
	response, err := json.Marshal(global_data)
	if err != nil {
		fmt.Fprintf(os.Stderr, "Could not convert to json for response\n")
	}
	fmt.Fprintf(w, string(response))
}

func deleteserver(w http.ResponseWriter, req *http.Request) {
	cors(&w, req)
	if req.Method == "OPTIONS" {
		return
	}
	if err := req.ParseForm(); err != nil {
		fmt.Fprintf(w, "ParseForm() err: %v", err)
		return
	}
	id, _ := strconv.Atoi(req.FormValue("id"))
	fmt.Printf("id = %d\n", id)
}

func newserver(w http.ResponseWriter, req *http.Request) {
	cors(&w, req)
	if req.Method == "OPTIONS" {
		return
	}
	if err := req.ParseForm(); err != nil {
		fmt.Fprintf(w, "ParseForm() err: %v", err)
		return
	}
	id := req.FormValue("address")
	fmt.Printf("address = %s\n", id)
}

func main() {
	value, err := readDataFromFile()
	if err != nil {
		fmt.Fprintf(os.Stderr, "Could not read data history\n")
		return
	}
	global_data = *value
	http.HandleFunc("/client/data", getdata)
	http.HandleFunc("/client/newserver", newserver)
	http.HandleFunc("/client/deleteserver", deleteserver)
	http.ListenAndServe(":8000", nil)
}
