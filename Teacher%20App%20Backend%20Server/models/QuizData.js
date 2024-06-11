"use strict";
const mongoose = require('mongoose');
const { Schema } = mongoose;


// Option schema for nested option objects
const optionSchema = new Schema({
    id: { type: String, required: true },
    url: { type: String, required: false, default: "<NOT CREATED>" },
    text: { type: String, required: true }
}, {
    _id: false // Prevent Mongoose from creating an automatic _id for options
});

// Question schema for nested question objects
const questionSchema = new Schema({
    question: {
        id: { type: String, required: true },
        url: { type: String, required: false, default: "<NOT CREATED>" },
        text: { type: String, required: true }
    },
    options: [optionSchema],
    correct_option_id: { type: String, required: true }
}, {
    _id: false // Prevent Mongoose from creating an automatic _id for questions
});

// Main quiz schema
const quizSchema = new Schema({
    _id: { type: String, required: true },
    creation_time: { type: Number, default: -1 },
    isPullModel: {type: Boolean, default:false },
    isTeacherApp: {type: Boolean, default:false },
    isProcessed: {type: Boolean, default:false },
    isDeleted: {type: Boolean, default:false },
    language: { type: String, required: true },
    theme: { type: String, required: true },
    localTheme: { type: String, required: true },
    themeAudio: { type: String, required: false, default: "<NOT CREATED>" },
    title: { type: String, required: true },
    localTitle: { type: String, required: true },
    titleAudio: { type: String, required: false, default: "<NOT CREATED>"  },
    positiveMarks: { type: Number, required: true },
    negativeMarks: { type: Number, required: true },
    questions: [questionSchema]
}, {
    collection: 'quizData'
});

var QuizData = (module.exports = mongoose.model('QuizData', quizSchema));

module.exports.getAllQuizData = () => {
    return QuizData.find().sort({creation_time: -1}).exec()
}

module.exports.getQuizById = id => {
  return QuizData.findOne({ id }).exec()
}

module.exports.fromQuizCreateRequest = (quizRequest) => {
    // Create a new QuizData object
    const quizData = new QuizData({
        _id: quizRequest.id,
        language: quizRequest.language,
        isPullModel: quizRequest.isPullModel,
        isTeacherApp: quizRequest.isTeacherApp,
        theme: quizRequest.theme,
        localTheme: quizRequest.localTheme,
        themeAudio: quizRequest.quizAudioData ? quizRequest.quizAudioData.themeAudio : "<NOT CREATED>",
        title: quizRequest.title,
        localTitle: quizRequest.localTitle,
        titleAudio: quizRequest.quizAudioData ? quizRequest.quizAudioData.titleAudio : "<NOT CREATED>",
        positiveMarks: quizRequest.positiveMark,
        negativeMarks: quizRequest.negativeMark,
        questions: quizRequest.questions.map((questionText, index) => ({
            question: {
                id: `${quizRequest.id}-q${index + 1}`,
                url: quizRequest.quizAudioData && quizRequest.quizAudioData.questionAudios ? quizRequest.quizAudioData.questionAudios[index] : "<NOT CREATED>",
                text: questionText
            },
            options: quizRequest.options[index].map((optionText, optionIndex) => ({
                id: `${quizRequest.id}-q${index + 1}-opt${optionIndex + 1}`,
                url: quizRequest.quizAudioData && quizRequest.quizAudioData.optionsAudios && quizRequest.quizAudioData.optionsAudios[index] ? quizRequest.quizAudioData.optionsAudios[index][optionIndex] : "<NOT CREATED>",
                text: optionText
            })),
            correct_option_id: `${quizRequest.id}-q${index + 1}-opt${quizRequest.correctAnswers[index] + 1}`
        }))
    });

    return quizData;
}
