import { Link, useNavigate } from "react-router-dom";
import { useState, useEffect } from "react";
import { v4 as uuidv4 } from "uuid";

const AddQuiz = ({ quiz }) => {
  const [inputFields, setInputFields] = useState([
    { question: "", optionA: "", optionB: "", optionC: "", optionD: "" },
  ]);

  const [metadata, setMetadata] = useState({
    title: "",
    language: "Kannada",
    positiveMark: 1,
    negativeMark: 0,
  });

  console.log("ADDQUIZ QUIZ", quiz);
  useEffect(() => {
    console.log("QUIZ PASSED VALUE", quiz);
    if (quiz && Object.keys(quiz).length > 0) {
      const quizMetadata = {
        title: quiz.title,
        language: quiz.language,
        positiveMark: quiz.positiveMark,
        negativeMark: quiz.negativeMark,
      };
      console.log("quizMetadata", quizMetadata);
      setMetadata(quizMetadata);
      // var a = []
      const a = quiz.options.map((option, index) => ({
        question: quiz.questions[index],
        optionA: option[0],
        optionB: option[1],
        optionC: option[2],
        optionD: option[3],
      }));
      console.log("inputFields", a);
      console.log("QUIZ WAS PASSED", quiz);
      setInputFields(a);
    } else {
      console.log("QUIZ WAS NOT PASSED", quiz);
    }
  }, [quiz]);

  const navigate = useNavigate();

  const handleFormChange = (index, event) => {
    let data = [...inputFields];
    data[index][event.target.name] = event.target.value;
    setInputFields(data);
  };

  const createQuizJson = () => {
    // const newMetadata = {...metadata[0]}
    metadata["questions"] = inputFields.map((mcq) => mcq.question);
    const options = inputFields.map((mcq) => [
      mcq.optionA,
      mcq.optionB,
      mcq.optionC,
      mcq.optionD,
    ]);
    const correctAnswers = Array(options.length).fill(0);
    metadata["correctAnswers"] = correctAnswers;
    metadata["options"] = options;
    metadata["type"] = "quiz";
    if (quiz) {
      metadata["id"] = quiz.id;
    } else {
      metadata["id"] = uuidv4();
    }
    //console.log("yo", metadata);
  };

  const isValid = () => {
    var valid = true;
    if (metadata.title.length == 0) {
      valid = false;
      alert("Title cannot be empty");
    } else if (metadata.language.length == 0) {
      valid = false;
      alert("Language cannot be empty");
    } else if (metadata.positiveMark.length == 0) {
      valid = false;
      alert("Positive marks cannot be empty");
    } else if (metadata.negativeMark.length == 0) {
      valid = false;
      alert("Negative marks cannot be empty");
    } else {
      inputFields.map((mcq, index) => {
        if (
          mcq.question.length == 0 ||
          mcq.optionA.length == 0 ||
          mcq.optionB.length == 0 ||
          mcq.optionC.length == 0 ||
          mcq.optionD.length == 0
        ) {
          valid = false;
          alert(`Question ${index + 1} is incomplete`);
        }
      });
    }
    return valid;
  };

  const onSubmit = (e) => {
    e.preventDefault();
    console.log("inputFields", inputFields);
    console.log("metatdata", metadata);
    createQuizJson();

    if (isValid()) {
      fetch("https://place-seeds.azurewebsites.net/create", {
        method: "POST",
        headers: { "content-type": "application/json" },
        body: JSON.stringify(metadata),
      })
        .then((res) => {
          alert("Saved successfully.");
          navigate("/content");
        })
        .catch((err) => {
          console.log(err.message);
        });
    }
  };

  const addFields = () => {
    let newfield = {
      question: "",
      optionA: "",
      optionB: "",
      optionC: "",
      optionD: "",
    };
    setInputFields([...inputFields, newfield]);
  };
  //     "positiveMark" : 1,    "negativeMark" : 0,    "id" : "Ramayana quiz 2",    "language" : "Kannada",
  const removeFields = (index) => {
    let data = [...inputFields];
    data.splice(index, 1);
    setInputFields(data);
  };

  return (
    <form onSubmit={onSubmit}>
      <div className="metadataGrid">
        <div>
          <label>Title</label>
          <br />
          <input
            className="mintgreen"
            type="text"
            name="title"
            placeholder=" Add Title"
            value={metadata.title || ""}
            onChange={(event) =>
              setMetadata({ ...metadata, title: event.target.value })
            }
          />
        </div>

        <div>
          <label>
            Language
            <br />
            <select
              value={metadata.language || ""}
              onChange={(event) =>
                setMetadata({ ...metadata, language: event.target.value })
              }
              className="mintgreen"
              style={{ width: "200px" }}
            >
              <option value="kannada">Kannada</option>
              <option value="hindi">Hindi</option>
              <option value="marathi">Marathi</option>
              <option value="english">English</option>
              <option value="tamil">Tamil</option>
              <option value="bengali">Bengali</option>
            </select>
          </label>
        </div>

        <div>
          <label>Positive Marks</label>
          <br />
          <input
            type="number"
            className="mintgreen"
            name="positiveMark"
            placeholder="Add Positive Marks"
            value={metadata.positiveMark || 1}
            onChange={(event) =>
              setMetadata({ ...metadata, positiveMark: event.target.value })
            }
          />
        </div>

        <div>
          <label>Negative Marks</label>
          <br />
          <input
            type="number"
            className="mintgreen"
            name="negativeMark"
            placeholder="Add Negative Marks"
            value={metadata.negativeMark || 0}
            onChange={(event) =>
              setMetadata({ ...metadata, negativeMark: event.target.value })
            }
          />
        </div>
      </div>
      {inputFields.map((input, index) => {
        return (
          <div key={index} style={{ marginTop: "1%" }}>
            <div className="optionsGrid">
              <div>
                <label>Question {index + 1}</label>
                <br />
                <input
                  type="text"
                  className="mintgreen"
                  name="question"
                  placeholder=" Add Question"
                  value={input.question}
                  onChange={(event) => handleFormChange(index, event)}
                />
              </div>
              <div>
                <button
                  className="btn"
                  type="button"
                  style={{ backgroundColor: "#28574F", color: "white" }}
                  onClick={() => removeFields(index)}
                >
                  Remove
                </button>
              </div>
              <div>
                <label>Option A (Correct Answer) </label>
                <br />
                <input
                  type="text"
                  name="optionA"
                  className="mintgreen"
                  placeholder=" Add Option A"
                  value={input.optionA}
                  onChange={(event) => handleFormChange(index, event)}
                />
              </div>
              <div>
                <label>Option B</label>
                <br />
                <input
                  type="text"
                  className="mintgreen"
                  placeholder=" Add Option B"
                  name="optionB"
                  value={input.optionB}
                  onChange={(event) => handleFormChange(index, event)}
                />
              </div>
              <div>
                <label>Option C</label>
                <br />
                <input
                  type="text"
                  className="mintgreen"
                  name="optionC"
                  placeholder=" Add Option C"
                  value={input.optionC}
                  onChange={(event) => handleFormChange(index, event)}
                />
              </div>
              <div>
                <label>Option D</label>
                <br />
                <input
                  type="text"
                  className="mintgreen"
                  name="optionD"
                  placeholder=" Add Option D"
                  value={input.optionD}
                  onChange={(event) => handleFormChange(index, event)}
                />
              </div>
            </div>
            <br />
          </div>
        );
      })}
      <button
        type="button"
        className="btn"
        style={{ backgroundColor: "#28574F", color: "white" }}
        onClick={addFields}
      >
        + Question
      </button>
      <br />
      <br />
      <input
        type="submit"
        style={{ backgroundColor: "#E5A83B", color: "white" }}
        value="Save"
        className="btn btn-block"
      />
    </form>
  );
};

export default AddQuiz;
