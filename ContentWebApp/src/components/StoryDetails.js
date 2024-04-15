import React from "react";
import { useState, useEffect } from "react";
import { SEEDS_URL } from "../Constants";

const StoryDetails = ({ type, story }) => {
  const [audioSrc, setAudioSrc] = useState('');
  const [answerAudioSrc, setAnswerAudioSrc] = useState('');

  useEffect(() => {
    const fetchSASUrl = async (url) => {
      try {
        const response = await fetch(`${SEEDS_URL}/content/sasUrl?url=${encodeURIComponent(url)}`, {
          method: 'GET',
          headers: {
            authToken: 'postman',
          },
        });
        const data = await response.json();
        return data.url;
      } catch (error) {
        console.error('Error fetching SAS URL:', error);
        return ''; // Return empty string on error
      }
    };

    const initialSrc = `https://seedsblob.blob.core.windows.net/output-container/${story.id}/1.0.wav`;
    const initialAnswerSrc = `https://seedsblob.blob.core.windows.net/output-container/${story.id}/answer/1.0.wav`;
    let questionSrc = `https://seedsblob.blob.core.windows.net/output-container/${story.id}/question/1.0.wav`;

    if (type === 'Riddle') {
      fetchSASUrl(questionSrc).then(setAudioSrc);
      fetchSASUrl(initialAnswerSrc).then(setAnswerAudioSrc);
    } else {
      fetchSASUrl(initialSrc).then(setAudioSrc);
    }
  }, [story.id, type]);

  return (
    <>
      <h2>{story.type}</h2>
      <br />
      <div className="metadataGrid">
        <div style={{ paddingBottom: "30px" }}>
          <div>Title</div>
          <div />
          <h4>{story.title}</h4>
          <h4>{story.localTitle}</h4>
        </div>
        <div style={{ paddingBottom: "30px" }}>
          <div>Language</div>
          <div />
          <h4>{story.language}</h4>
        </div>
        <div style={{ paddingBottom: "30px" }}>
          <div>Uploaded on</div>
          <div />
          {story.isPullModel && <h4>IVR</h4>}
          {story.isTeacherApp && <h4>Teacher App</h4>}
        </div>
      </div>
      {story.description && (
        <div style={{ paddingBottom: "30px" }}>
          <div>Description</div>
          <div />
          <h4>{story.description}</h4>
        </div>
      )}
      <div style={{ paddingBottom: "30px" }}>
        <div>Theme</div>
        <div />
        <h4>{story.theme}</h4>
        <h4>{story.localTheme}</h4>
      </div>
      {story.isProcessed && (
        <div style={{ paddingBottom: "30px" }}>
          Audio: <br /> <audio controls src={audioSrc} />
        </div>
      )}
      {story.isProcessed && type == "Riddle" && (
        <div style={{ paddingBottom: "30px" }}>
          Answer Audio: <br /> <audio controls src={answerAudioSrc} />
        </div>
      )}

      {!story.isProcessed && <h6>Audio is being processed</h6>}
    </>
  );
};

export default StoryDetails;

// For story, etc. => Teacher app ticked
