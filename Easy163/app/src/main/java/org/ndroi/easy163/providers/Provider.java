package org.ndroi.easy163.providers;

import android.util.Log;
import com.alibaba.fastjson.JSONObject;
import org.ndroi.easy163.providers.utils.BitRate;
import org.ndroi.easy163.providers.utils.KeywordMatch;
import org.ndroi.easy163.utils.ReadStream;
import org.ndroi.easy163.utils.Keyword;
import org.ndroi.easy163.utils.Song;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public abstract class Provider {

  protected Keyword targetKeyword;
  protected int selectedIndex = -1;
  protected List<Keyword> candidateKeywords = new ArrayList<>();
  protected List<JSONObject> songJsonObjects = new ArrayList<>();

  public Provider(Keyword targetKeyword) {
    this.targetKeyword = targetKeyword;
  }

  @Override
  public String toString() {
    return getClass().getSimpleName();
  }

  static protected String keyword2Query(Keyword keyword) {
    StringBuilder singers = new StringBuilder();

    for (String singer : keyword.singers) {
      singers.append(singer).append(" ");
    }

    singers.substring(0, singers.length() - 1);

    if (singers.toString().split(" ").length >= 3) {
      singers = new StringBuilder();
      Log.d("keyword2Query", "too many spaces singer string, aborted");
    }

    String queryStr = keyword.songName + " " + singers;

    try {
      queryStr = URLEncoder.encode(queryStr, "UTF-8");
    } catch (UnsupportedEncodingException e) {
      e.printStackTrace();
    }
    return queryStr;
  }

  static private int calculateScore(Keyword candidateKeyword, Keyword targetKeyword, int index) {
    if (!KeywordMatch.match(candidateKeyword, targetKeyword)) {
      return -100;
    }

    int score = 5 - 3 * index;
    String targetName = targetKeyword.songName.toLowerCase();
    String candidateSongName = candidateKeyword.songName.toLowerCase();
    int candidateLen = candidateSongName.length();
    int targetLen = targetName.length();
    score -= Math.abs(candidateLen - targetLen);
    String leftName = candidateSongName.replace(targetName, "");
    List<String> words = Arrays.asList(
        "live", "dj", "remix", "cover", "instrumental", "伴奏", "翻唱", "翻自"
    );

    for (String word : words) {
      if (KeywordMatch.match(word, leftName)) {
        if (KeywordMatch.match(word, targetKeyword.extra)) {
          score = 7;
        } else {
          score -= 2;
        }
      }
    }

    score -= Math.abs(targetKeyword.singers.size() - candidateKeyword.singers.size());

    for (String targetSinger : targetKeyword.singers) {
      for (String candidateSinger : candidateKeyword.singers) {
        if (KeywordMatch.match(targetSinger, candidateSinger)) {
          score += 2;
          score -= 2 * Math.abs(targetSinger.length() - candidateSinger.length());
        }
      }
    }
    return score;
  }

  static public Provider selectCandidateKeywords(List<Provider> providers) {
    Provider bestProvider = null;
    int maxScore = -100;
    int selectIndex = -1;

    for (Provider provider : providers) {
      for (int i = 0; i < provider.candidateKeywords.size(); i++) {
        Keyword candidateKeyword = provider.candidateKeywords.get(i);
        int score = calculateScore(candidateKeyword, provider.targetKeyword, i);
        Log.d("calculateScore", candidateKeyword.toString() + '|' + provider.targetKeyword.toString() + "|" + score);

        if (score > maxScore) {
          maxScore = score;
          selectIndex = i;
          bestProvider = provider;
        }
      }
    }

    if (bestProvider != null) {
      bestProvider.selectedIndex = selectIndex;
    }
    return bestProvider;
  }

  static protected Song generateSong(String url) {
    Song song = null;

    try {
      HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
      connection.setRequestMethod("GET");
      connection.setRequestProperty("range", "bytes=0-8191");
      connection.connect();
      int responseCode = connection.getResponseCode();

      if (responseCode == HttpURLConnection.HTTP_OK ||
          responseCode == HttpURLConnection.HTTP_PARTIAL) {
        song = new Song();
        song.url = url;
        String content_range = connection.getHeaderField("Content-Range");

        if (content_range != null) {
          int p = content_range.indexOf('/');
          song.size = Integer.parseInt(content_range.substring(p + 1));
        } else {
          song.size = connection.getContentLength();
        }

        String qqMusicMd5 = connection.getHeaderField("Server-Md5");

        if (qqMusicMd5 != null) {
          song.md5 = qqMusicMd5;
        }

        byte[] mp3Data = ReadStream.read(connection.getInputStream());
        song.br = BitRate.Detect(mp3Data);
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
    return song;
  }

  abstract public void collectCandidateKeywords();

  abstract public Song fetchSelectedSong();
}
