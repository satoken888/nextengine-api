package jp.co.kawakyo.nextengineapi.base;
/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
import lombok.Data;

/**
 * This class holds Authentication Token of User
 */
@Data
public class NeToken implements java.io.Serializable {

  private String accessToken = null;
  private String refreshToken = null;
}