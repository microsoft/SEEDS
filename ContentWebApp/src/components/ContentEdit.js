import { useState } from "react";
import AddQuiz from "./AddQuiz";
import AddStory from "./AddStory";
import { useLocation } from "react-router-dom";
import { useEffect } from "react";
import { Link, useParams } from "react-router-dom";
import { useNavigate } from "react-router-dom";
import { SEEDS_URL } from "../Constants";

const ContentEdit = () => {
  const { type, id } = useParams();
  // console.log(type, id);
  const [content, setContent] = useState({});
  const [experience, setExperience] = useState("quiz");
  const navigate = useNavigate();

  useEffect(() => {
    const getContentById = async () => {
      const contentFromServer = await contentById();
      setContent(contentFromServer);
      console.log("quizInEdit", contentFromServer);
      setExperience(contentFromServer.type);
    };
    getContentById();
  }, []);

  const contentById = async () => {
    // const res = await fetch("http://localhost:5001/content");

    if (type == "quiz") {
      const placeRes = await fetch(
        "https://place-seeds.azurewebsites.net/rawDataById?" +
          new URLSearchParams({
            id: id,
          })
      );
      const data = await placeRes.json();
      console.log(data);
      return data;
    } else {
      const seedsRes = await fetch(`${SEEDS_URL}/content/${id}`, {
        method: "GET",
        headers: {
          authToken: "postman",
        },
      });
      const seedsData = await seedsRes.json();
      return seedsData;
    }
  };

  const location = useLocation();
  console.log("link props", location.state);

  const handleChange = (event) => {
    setExperience(event.target.value);
    console.log(event.target.value);
  };

  if (
    content &&
    !content.isProcessed
  ) {
    return (
      <>
        <div style={{ margin: "20px" }}>
          <h3>{content.title}</h3>
          <p>Content is being processed, try again later!</p>
        </div>
      </>
    );
  } else {
    return (
      <>
        <div style={{ margin: "20px" }}>
          <h3>Edit Content</h3>
          {content &&
            (experience == "Story" ||
              experience == "Poem" ||
              experience == "Song") && (
              <form>
                <label>
                  Experience:
                  <select
                    value={experience}
                    onChange={(event) => handleChange(event)}
                    className="mintgreen"
                    style={{ width: "150px" }}
                  >
                    <option value="Story">Story</option>
                    <option value="Poem">Poem</option>
                    <option value="Song">Song</option>
                  </select>
                </label>
              </form>
            )}
          {content && experience == "quiz" && content.isProcessed && <AddQuiz quiz={content} />}
          {content &&
            (experience != "quiz") &&
            content.isProcessed && (
              <AddStory content={content} contentType={experience} />
            )}
          <div />
        </div>
      </>
    );
  }
};

export default ContentEdit;
