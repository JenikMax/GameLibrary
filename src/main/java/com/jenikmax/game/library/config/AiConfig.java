package com.jenikmax.game.library.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
@ConfigurationProperties(prefix = "ai")
public class AiConfig {

    private String modelsDir;
    private Embedding embedding = new Embedding();
    private Translation translation = new Translation();
    private AutoTag autoTag = new AutoTag();

    public String getModelsDir() { return modelsDir; }
    public void setModelsDir(String modelsDir) { this.modelsDir = modelsDir; }
    public Embedding getEmbedding() { return embedding; }
    public void setEmbedding(Embedding embedding) { this.embedding = embedding; }
    public Translation getTranslation() { return translation; }
    public void setTranslation(Translation translation) { this.translation = translation; }
    public AutoTag getAutoTag() { return autoTag; }
    public void setAutoTag(AutoTag autoTag) { this.autoTag = autoTag; }

    public static class Embedding {
        private boolean enabled = true;
        private String modelFile;
        private String vocabFile;
        private int dimension = 384;
        private int maxLength = 512;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public String getModelFile() { return modelFile; }
        public void setModelFile(String modelFile) { this.modelFile = modelFile; }
        public String getVocabFile() { return vocabFile; }
        public void setVocabFile(String vocabFile) { this.vocabFile = vocabFile; }
        public int getDimension() { return dimension; }
        public void setDimension(int dimension) { this.dimension = dimension; }
        public int getMaxLength() { return maxLength; }
        public void setMaxLength(int maxLength) { this.maxLength = maxLength; }
    }

    public static class Translation {
        private boolean enabled = true;
        private Map<String, TranslationModel> models;
        private int maxLength = 512;
        private int beamSize = 4;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public Map<String, TranslationModel> getModels() { return models; }
        public void setModels(Map<String, TranslationModel> models) { this.models = models; }
        public int getMaxLength() { return maxLength; }
        public void setMaxLength(int maxLength) { this.maxLength = maxLength; }
        public int getBeamSize() { return beamSize; }
        public void setBeamSize(int beamSize) { this.beamSize = beamSize; }
    }

    public static class TranslationModel {
        private String modelFile;
        private String vocabFile;

        public String getModelFile() { return modelFile; }
        public void setModelFile(String modelFile) { this.modelFile = modelFile; }
        public String getVocabFile() { return vocabFile; }
        public void setVocabFile(String vocabFile) { this.vocabFile = vocabFile; }
    }

    public static class AutoTag {
        private boolean enabled = true;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
    }
}
