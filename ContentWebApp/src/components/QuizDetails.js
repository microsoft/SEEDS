import { useState, useEffect } from "react";

const QuizDetails = ({ quiz }) => {
  console.log(quiz);
  return (
    <>
    <h2>Quiz</h2>
      <div className="metadataGrid">
        <div>
          <div>Title</div>
          <p><b>{quiz.title}</b></p>
        </div>

        <div>
          <div>Language</div>
          <p><b>{quiz.language}</b></p>
        </div>

        <div>
          <label>Positive Marks</label>
          <br />
          <p className="mintgreen" style={{width:"100px", textAlign: "center"}}>{quiz.positiveMark}</p>
        </div>

        <div>
          <label>Negative Marks</label>
          <br />
          <p className="mintgreen" style={{width:"100px", textAlign: "center"}}>{quiz.negativeMark}</p>
        </div>
      </div>
      {quiz.questions.map((question, index) => {
        return (
          <div key={index} style={{ marginTop: "1%" }}>
            <div>
              <label>Question {index + 1}</label>
              <br />
              <p style={{fontWeight: "700"}}>{question}</p>
            </div>
            <div className="optionsDetailsGrid">
              <div>
                <label>Option A (Correct Answer) </label>
                <br />
                <p className="mintgreen">{quiz.options[index][0]}</p>
              </div>
              <div>
                <label>Option B</label>
                <br />
                <p className="mintgreen">{quiz.options[index][1]}</p>
              </div>
              <div>
                <label>Option C</label>
                <br />
                <p className="mintgreen">{quiz.options[index][2]}</p>
              </div>
              <div>
                <label>Option D</label>
                <br />
                <p className="mintgreen">{quiz.options[index][3]}</p>
              </div>
            </div>
            <br />
          </div>
        );
      })}
    </>
  );
};

export default QuizDetails;
