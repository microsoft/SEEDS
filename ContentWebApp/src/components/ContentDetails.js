import React from "react";
import { useState, useEffect } from "react";
import { Link, useParams } from "react-router-dom";
import QuizDetails from "./QuizDetails";
import StoryDetails from "./StoryDetails";
import { SEEDS_URL } from "../Constants";

const ContentDetails = () => {
  const { type, id } = useParams();
  // console.log(type, id);
  const [content, setContent] = useState(null);

  useEffect(() => {
    console.log("useEffect");
    const getContentById = async () => {
      const contentFromServer = await contentById();
      setContent(contentFromServer);
      console.log("hello", contentFromServer);
    };
    getContentById();
  }, []);

  const contentById = async () => {
    console.log("CONTENTBYID", type);
    // console.log(type)
    // const res = await fetch("http://localhost:5001/content");

    if (type == "quiz") {
      const placeRes = await fetch(
        "https://place-seeds.azurewebsites.net/rawDataById?" +
          new URLSearchParams({
            id: id,
          })
      );
      const data = await placeRes.json();
      console.log("ContentDetailsData", data);
      return data;
    } else {
      const seedsRes = await fetch(
        `${SEEDS_URL}/content/${id}`,
        {
          method: "GET",
          headers: {
            authToken: "postman",
          },
        }
      );
      const seedsData = await seedsRes.json();
      console.log("ContentDetailsData1", seedsData);
      return seedsData;
    }
  };

  if (content && !content.isProcessed) {
    return (
      <>
        <div style={{ margin: "20px" }}>
          <h3>Title: {content.title}</h3>
          <p>Content is being processed, try again later!</p>
        </div>
      </>
    );
  } else {
    return (
      <div style={{ margin: "20px" }}>
        {content && content.isProcessed && content.type == "quiz" && (
          <QuizDetails quiz={content} />
        )}
        {content &&
          content.isProcessed &&
          (content.type != "quiz") && <StoryDetails type={content.type} story={content} />}
      </div>
    );
  }
};

export default ContentDetails;
