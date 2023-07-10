import React from "react";

const StoryDetails = ({ type, story }) => {
  var src = `https://seedscontent.blob.core.windows.net/output-original/${story.id}.mp3`;
  var answerSrc = `https://seedscontent.blob.core.windows.net/output-original/${story.id}/answer.mp3`;
  if (type == "Riddle") {
    src = `https://seedscontent.blob.core.windows.net/output-original/${story.id}/question.mp3`;
  }
  console.log(story);
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
          Audio: <br /> <audio controls src={src} />
        </div>
      )}
      {story.isProcessed && type == "Riddle" && (
        <div style={{ paddingBottom: "30px" }}>
          Answer Audio: <br /> <audio controls src={answerSrc} />
        </div>
      )}

      {!story.isProcessed && <h6>Audio is being processed</h6>}
    </>
  );
};

export default StoryDetails;

// For story, etc. => Teacher app ticked
