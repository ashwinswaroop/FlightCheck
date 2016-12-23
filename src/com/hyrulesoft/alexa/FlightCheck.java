package com.hyrulesoft.alexa;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazon.speech.slu.Intent;
import com.amazon.speech.slu.Slot;
import com.amazon.speech.speechlet.IntentRequest;
import com.amazon.speech.speechlet.LaunchRequest;
import com.amazon.speech.speechlet.Session;
import com.amazon.speech.speechlet.SessionEndedRequest;
import com.amazon.speech.speechlet.SessionStartedRequest;
import com.amazon.speech.speechlet.Speechlet;
import com.amazon.speech.speechlet.SpeechletException;
import com.amazon.speech.speechlet.SpeechletResponse;
import com.amazon.speech.ui.PlainTextOutputSpeech;
import com.amazon.speech.ui.Reprompt;
import com.amazon.speech.ui.SimpleCard;

public class FlightCheck implements Speechlet {
private static final Logger log = LoggerFactory.getLogger(FlightCheck.class);

/**
 * Array containing space facts.
 */

@Override
public void onSessionStarted(final SessionStartedRequest request, final Session session)
        throws SpeechletException {
    log.info("onSessionStarted requestId={}, sessionId={}", request.getRequestId(),
            session.getSessionId());
}

@Override
public SpeechletResponse onLaunch(final LaunchRequest request, final Session session)
        throws SpeechletException {
    log.info("onLaunch requestId={}, sessionId={}", request.getRequestId(),
            session.getSessionId());
    return getHelpResponse();
}

@Override
public SpeechletResponse onIntent(final IntentRequest request, final Session session)
        throws SpeechletException {
    log.info("onIntent requestId={}, sessionId={}", request.getRequestId(),
            session.getSessionId());

    Intent intent = request.getIntent();
    String intentName = (intent != null) ? intent.getName() : null;
    Slot Airline = null;
    Slot FlightNum = null;
    Slot Destination = null;

    if ("CheckFlight".equals(intentName)) {
    	
    	Airline = intent.getSlot("Airline");
    	FlightNum = intent.getSlot("FlightNum");
    	Destination = intent.getSlot("Destination");
        try {
			return getNewCheckFlightResponse(Airline,FlightNum,Destination);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (com.amazonaws.util.json.JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        throw new SpeechletException("Invalid Flight Number");

    } else {
        throw new SpeechletException("Invalid Intent");
    }
}

@Override
public void onSessionEnded(final SessionEndedRequest request, final Session session)
        throws SpeechletException {
    log.info("onSessionEnded requestId={}, sessionId={}", request.getRequestId(),
            session.getSessionId());
    // any cleanup logic goes here
}

/**
 * Gets a random new fact from the list and returns to the user.
 * @throws com.amazonaws.util.json.JSONException 
 * @throws IOException 
 */
private SpeechletResponse getNewCheckFlightResponse(Slot Airline, Slot FlightNum, Slot Destination) throws IOException, com.amazonaws.util.json.JSONException {

	String arrivalTime = getArrivalTime(FlightNum.getValue());
	
    String speechText = "Flight "+FlightNum.getValue()+" is scheduled to arrive at "+arrivalTime;

    // Create the Simple card content.
    SimpleCard card = new SimpleCard();
    card.setTitle("FlightCheck");
    card.setContent(speechText);

    // Create the plain text output.
    PlainTextOutputSpeech speech = new PlainTextOutputSpeech();
    speech.setText(speechText);

    return SpeechletResponse.newTellResponse(speech, card);
}

/**
 * Returns a response for the help intent.
 */
private SpeechletResponse getHelpResponse() {
    String speechText =
            "For example, you can ask Flight Check what is the status of Lufthansa flight 112";
    // Create the plain text output.
    PlainTextOutputSpeech speech = new PlainTextOutputSpeech();
    speech.setText(speechText);

    // Create reprompt
    Reprompt reprompt = new Reprompt();
    reprompt.setOutputSpeech(speech);

    return SpeechletResponse.newAskResponse(speech, reprompt);
}

public String getArrivalTime(String flightNum) throws IOException, com.amazonaws.util.json.JSONException {
	
	URL url = new URL("http://flightxml.flightaware.com/json/FlightXML2/FlightInfo?ident="+flightNum+"&howMany=1");
	String userPassword = "swarooprao:baf818d30d41d7670790c4112afd40501b4329fd";
	String encoding = new String(Base64.encodeBase64(userPassword.getBytes()));
	URLConnection uc = url.openConnection();
	uc.setRequestProperty("Authorization", "Basic " + encoding);
	uc.connect();
	StringBuilder JSON_DATA = new StringBuilder();
	BufferedReader br = new BufferedReader(new InputStreamReader(uc
            .getInputStream()));
        String l = null;
        while ((l=br.readLine())!=null) {
            JSON_DATA = JSON_DATA.append(l);
        }
        br.close();
        System.out.println(JSON_DATA);
        com.amazonaws.util.json.JSONObject obj = new com.amazonaws.util.json.JSONObject(JSON_DATA.toString());
        String a = obj.getJSONObject("FlightInfoResult").getJSONArray("flights").getJSONObject(0).getString("estimatedarrivaltime");
        System.out.println(a);
        Date date = new Date(Long.parseLong(a));
        SimpleDateFormat formatter = new SimpleDateFormat("MM/dd/yyyy");

        formatter = new SimpleDateFormat("hh:mm a");
        formatter.setTimeZone(TimeZone.getTimeZone("America/Detroit"));;
        String time = formatter.format(date);
        return time;
}
}

