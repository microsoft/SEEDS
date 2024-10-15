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
