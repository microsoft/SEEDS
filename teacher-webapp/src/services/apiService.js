const api_base = process.env.REACT_APP_CONF_SERVER_BASE_URI + '/conference';

export const createConference = async (teacherPhone, studentPhones) => {
  const response = await fetch(`${api_base}/create`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({
      teacher_phone: teacherPhone,
      student_phones: studentPhones,
    }),
  });
  return response.json();
};

export const startConferenceCall = async (confId) => {
  return fetch(`${api_base}/start/${confId}`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
  });
};

export const endConferenceCall = async (confId) => {
  return fetch(`${api_base}/end/${confId}`, {
    method: 'PUT',
    headers: {
      'Content-Type': 'application/json',
    },
  });
};

export const muteParticipant = async (confId, phone_number) => {
  return fetch(`${api_base}/muteparticipant/${confId}?phone_number=${phone_number}`, {
    method: 'PUT',
    headers: {
      'Content-Type': 'application/json',
    },
  });
};

export const unmuteParticipant = async (confId, phone_number) => {
  return fetch(`${api_base}/unmuteparticipant/${confId}?phone_number=${phone_number}`, {
    method: 'PUT',
    headers: {
      'Content-Type': 'application/json',
    },
  });
};

export const playAudio = async (confId) => {
  const url =  `https://${process.env.REACT_APP_STORAGE_ACCOUNT_NAME}.blob.core.windows.net/output-container/25/1.0.wav`
  return fetch(`${api_base}/playaudio/${confId}?url=${url}`, {
    method: 'PUT',
    headers: {
      'Content-Type': 'application/json',
    },
  });
};

export const pauseAudio = async (confId) => {
  return fetch(`${api_base}/pauseaudio/${confId}`, {
    method: 'PUT',
    headers: {
      'Content-Type': 'application/json',
    },
  });
};

export const addParticipant = async (confId, phone_number) => {
  return fetch(`${api_base}/addparticipant/${confId}?phone_number=${phone_number}`, {
    method: 'PUT',
    headers: {
      'Content-Type': 'application/json',
    },
  });
}