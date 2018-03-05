package com.github.tteofili.jtm;

import java.io.InputStream;

import org.deeplearning4j.text.tokenization.tokenizer.TokenPreProcess;
import org.deeplearning4j.text.tokenization.tokenizer.Tokenizer;
import org.deeplearning4j.text.tokenization.tokenizerfactory.TokenizerFactory;

/**
 *
 */
public class OpenNlpTokenizerFactory implements TokenizerFactory {
  @Override
  public Tokenizer create(String toTokenize) {
    return null;
  }

  @Override
  public Tokenizer create(InputStream toTokenize) {
    return null;
  }

  @Override
  public void setTokenPreProcessor(TokenPreProcess preProcessor) {

  }

  @Override
  public TokenPreProcess getTokenPreProcessor() {
    return null;
  }
}
