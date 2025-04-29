package hk.ust.csit5930.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "search.engine")
public class SearchEngineConfig {
    private String rootUrl;
    private String stopwordsPath;
    private String bodyIndexDb;
    private String bodyIndexName;
    private int pagerankIterations;
    private double pagerankDampingFactor;

    public String getRootUrl() {
        return rootUrl;
    }

    public void setRootUrl(String rootUrl) {
        this.rootUrl = rootUrl;
    }

    public String getStopwordsPath() {
        return stopwordsPath;
    }

    public void setStopwordsPath(String stopwordsPath) {
        this.stopwordsPath = stopwordsPath;
    }

    public String getBodyIndexDb() {
        return bodyIndexDb;
    }

    public void setBodyIndexDb(String bodyIndexDb) {
        this.bodyIndexDb = bodyIndexDb;
    }

    public String getBodyIndexName() {
        return bodyIndexName;
    }

    public void setBodyIndexName(String bodyIndexName) {
        this.bodyIndexName = bodyIndexName;
    }

    public int getPagerankIterations() {
        return pagerankIterations;
    }

    public void setPagerankIterations(int pagerankIterations) {
        this.pagerankIterations = pagerankIterations;
    }

    public double getPagerankDampingFactor() {
        return pagerankDampingFactor;
    }

    public void setPagerankDampingFactor(double pagerankDampingFactor) {
        this.pagerankDampingFactor = pagerankDampingFactor;
    }
}