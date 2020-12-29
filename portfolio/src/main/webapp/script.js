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
        createBasicMap();
      };
      document.head.appendChild(script);
    });

/** Creates the basic map. */
function createBasicMap() {
  const map = new google.maps.Map(document.getElementById('map'), {
    center: {lat: -35.280, lng: 149.119},
    zoom: 15,
  });
}
