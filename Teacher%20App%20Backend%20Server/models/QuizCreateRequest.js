class QuizAudioData {
    constructor(data) {
        this.titleAudio = data.titleAudio ? data.titleAudio : null;
        this.themeAudio = data.themeAudio ? data.themeAudio : null;
        this.questionAudios = data.questionAudios ? data.questionAudios : null;
        this.optionsAudios = data.optionsAudios ? data.optionsAudios : null;
    }
}

class QuizCreateRequest {
    constructor(data) {
        this.id = data.id || 'default-id';
        this.isPullModel = data.isPullModel || true
        this.isTeacherApp = data.isTeacherApp || false
        this.title = data.title || 'Untitled Quiz';
        this.localTitle = data.localTitle || 'Untitled Quiz';
        this.theme = data.theme || 'Default theme'
        this.localTheme = data.localTheme || 'Default Local Theme';
        this.language = data.language || 'English';
        this.positiveMark = data.positiveMark || 1;
        this.negativeMark = data.negativeMark || 0;
        this.questions = data.questions || [];
        this.correctAnswers = data.correctAnswers || [];
        this.options = data.options || [[]];
        this.type = data.type || 'quiz';
        this.quizAudioData = data.quizAudioData ? new QuizAudioData(data.quizAudioData) : null;
    }
}

module.exports = QuizCreateRequest;