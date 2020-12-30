// Copyright 2019 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     https://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

/** Adds a random keyword of me to the page. */
function addRandomKeyword() {
  const keywords = [
    'Delicious Food', 'Pet Tortoises', '你好，谷歌！', 'Computer & Actuarial',
    'Travel', 'Disney Movies'
  ];

  // Pick a random keyword.
  const keyword = keywords[Math.floor(Math.random() * keywords.length)];

  // Add it to the page.
  const keywordContainer = document.getElementById('keyword-container');
  keywordContainer.innerText = keyword;
}

/** Fetches data from the server and adds it to the DOM. */
async function fetchData() {
  const response = await fetch('/data');
  const data = await response.text();
  document.getElementById('data-container').innerText = data;
}

/** Removes deleted data from the page. */
async function deleteData() {
  const response = await fetch('/delete-data');
  const data = await response.text();
  document.getElementById('data-container').innerText = data;
}

/** Gets the Blobstore upload URL from server using fetch and shows the form. */
function fetchBlobstoreUrlAndShowForm() {
  fetch('/blobstore-upload-url')
      .then((response) => {
        return response.text();
      })
      .then((imageUploadUrl) => {
        const messageForm = document.getElementById('image-upload-form');
        messageForm.action = imageUploadUrl;
        messageForm.classList.remove('hidden');
      });
}

/** Loads the API key from json file and attaches to html. */
fetch('./config.json')
    .then(response => {
      return response.json();
    })
    .then(data => {
      const script = document.createElement('script');
      script.src =
          'https://maps.googleapis.com/maps/api/js?key=' + data.api_key +
          '&callback=initMap&libraries=&v=weekly';
      script.defer = true;
      window.initMap = function() {
        createMap();
      };
      document.head.appendChild(script);
    });

/** Creates a map. */
function createMap() {
  const anuMap = new google.maps.Map(document.getElementById('map'), {
    center: {lat: -35.280, lng: 149.119},
    zoom: 15,
  });
  addMarkers(anuMap);
}

/** Adds personalised markers to the map. */
function addMarkers(myMap) {
  const iconBase = 'https://maps.google.com/mapfiles/kml/paddle/';
  const labMarker = new google.maps.Marker({
    position: {lat: -35.27528029, lng: 149.12073692},
    map: myMap,
    title: 'Computer Lab',
    icon: iconBase + 'purple-stars.png',
  });
  const libraryMarker = new google.maps.Marker({
    position: {lat: -35.27796177, lng: 149.12059922},
    map: myMap,
    title: 'Favorite Library',
    icon: iconBase + 'pink-stars.png',
  });
  addLibraryInfoWindow(myMap, libraryMarker);
}

/** Generates an info window for a specific marker. */
function generateInfoWindow(infoString, map, marker) {
  const infoWindow = new google.maps.InfoWindow({
    content: infoString,
  });
  marker.addListener('click', () => {
    infoWindow.open(map, marker);
  });
}

/** Adds an info window for the library marker. */
function addLibraryInfoWindow(map, libraryMarker) {
  // TODO: Investigate how to separate this HTML segment from this Javascript file. (Have tried to convert a html file into a string here while still having bugs.)
  const libraryInfoString = '<div id="content">\
      <div id="siteNotice"></div>\
      <h1 id="firstHeading" class="firstHeading">My Favorite Library</h1>\
      <div id="bodyContent">\
      <p>The <b>Chifley Library</b>, named after Joseph Benedict Chifley (Prime Minister of Australia from 1945-1949), \
      supports the teaching, learning and research activities of staff and students in ANU CASS and CBE.</p>\
      <p>Attribution: ANU Chifley Library, <a href="https://anulib.anu.edu.au/using-library/branches/chifley-library">\
      https://anulib.anu.edu.au/using-library/branches/chifley-library</a></p>\
      </div></div>';
  generateInfoWindow(libraryInfoString, map, libraryMarker);
}
