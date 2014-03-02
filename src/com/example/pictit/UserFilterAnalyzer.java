package com.example.pictit;

// TBD: Use well-known NLP cloud based APIs for named entity recognition
// For now lets use this dumb version

import android.util.Log;

public class UserFilterAnalyzer implements LogUtils{

	private String TAG = "pickit/UserFilterAnalyzer";

	private int	MATCH_SUCCESS = 0;
	private int	MATCH_PARTIAL = 1;
	private int	MATCH_FAILURE  = 2;

    private String userFilter = null;

    private String[] mWords ;

    public UserFilterAnalyzer(String filter) {
    	userFilter = filter;

    	mWords = userFilter.split("\\s+");
    	if (DEBUG) {
    		for (int i = 0; i < mWords.length; i++) {
    		  Log.d(TAG, "Word "+ i+1 + " :" + mWords[i]);
    		}
    	}
    }

    public int compareUserFilter(String compareString) {
    	if (DEBUG) Log.d(TAG, "Compare String : " + compareString);
    	String concat = "";
		for (int i = 0; i < mWords.length; i++) {
		  concat += mWords[i] + " ";
		  if (DEBUG) Log.d(TAG, "First Index : " + i + " - " + concat);
  		  for (int j = i+1; j < mWords.length; j++) {
  			concat += mWords[j] + " ";
  			if (DEBUG) Log.d(TAG, "Second Index : " + j + " - " + concat);
  			if(concat.toLowerCase().contains(compareString.toLowerCase())) {
  				return MATCH_SUCCESS;
  			}
  			if (j+1 == mWords.length) {
  				if (DEBUG) Log.d(TAG, "**** Reset ****");
  				concat = "";
  			}
  		  }
	      if(concat.toLowerCase().contains(compareString.toLowerCase())) {
			return MATCH_SUCCESS;
		  }
  		}
    	return MATCH_FAILURE;
    }

	@Override
	public String toString() {
	  // TODO Auto-generated method stub
	  return null;
	}
}