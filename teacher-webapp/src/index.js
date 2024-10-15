import React from 'react';
import ReactDOM from 'react-dom';
import App from './App';
import { ConferenceProvider } from './context/ConferenceContext'; // Import ConferenceProvider
import './index.css';

ReactDOM.render(
  <React.StrictMode>
    <ConferenceProvider>
      <App />
    </ConferenceProvider>
  </React.StrictMode>,
  document.getElementById('root')
);
