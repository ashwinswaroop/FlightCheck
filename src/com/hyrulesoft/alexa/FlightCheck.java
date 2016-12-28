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
import com.amazonaws.util.json.JSONArray;
import com.amazonaws.util.json.JSONObject;

public class FlightCheck implements Speechlet {
	private static final Logger log = LoggerFactory.getLogger(FlightCheck.class);

	/**
	 * Array containing space facts.
	 */

	@Override
	public void onSessionStarted(final SessionStartedRequest request, final Session session) throws SpeechletException {
		log.info("onSessionStarted requestId={}, sessionId={}", request.getRequestId(), session.getSessionId());
	}

	@Override
	public SpeechletResponse onLaunch(final LaunchRequest request, final Session session) throws SpeechletException {
		log.info("onLaunch requestId={}, sessionId={}", request.getRequestId(), session.getSessionId());
		return getHelpResponse();
	}

	@Override
	public SpeechletResponse onIntent(final IntentRequest request, final Session session) throws SpeechletException {
		log.info("onIntent requestId={}, sessionId={}", request.getRequestId(), session.getSessionId());

		Intent intent = request.getIntent();
		String intentName = (intent != null) ? intent.getName() : null;
		Slot Airline = null;
		Slot FlightNum = null;
		String speechText = "Invalid Flight Number or Airline Name";
		PlainTextOutputSpeech speech = new PlainTextOutputSpeech();
		speech.setText(speechText);

		if ("CheckFlight".equals(intentName)) {

			Airline = intent.getSlot("Airline");
			FlightNum = intent.getSlot("FlightNum");
			log.info("FlightNum = " + FlightNum.getValue());

			try {
				if(FlightNum.getValue()==null || Airline.getValue()==null || FlightNum.getValue().equals("?") || Airline.getValue().equals("?"))
					return SpeechletResponse.newTellResponse(speech);
				else
					return getNewCheckFlightResponse(Airline, FlightNum);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (com.amazonaws.util.json.JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			throw new SpeechletException("Something went wrong");	

		}
		else if ("AMAZON.HelpIntent".equals(intentName)) {
            return getHelpResponse();

        } else if ("AMAZON.StopIntent".equals(intentName)) {
            PlainTextOutputSpeech outputSpeech = new PlainTextOutputSpeech();
            outputSpeech.setText("Goodbye");

            return SpeechletResponse.newTellResponse(outputSpeech);
        } else if ("AMAZON.CancelIntent".equals(intentName)) {
            PlainTextOutputSpeech outputSpeech = new PlainTextOutputSpeech();
            outputSpeech.setText("Goodbye");

            return SpeechletResponse.newTellResponse(outputSpeech);
        }else {
        	return SpeechletResponse.newTellResponse(speech);
		}
	}

	@Override
	public void onSessionEnded(final SessionEndedRequest request, final Session session) throws SpeechletException {
		log.info("onSessionEnded requestId={}, sessionId={}", request.getRequestId(), session.getSessionId());
		// any cleanup logic goes here
	}

	/**
	 * Gets a random new fact from the list and returns to the user.
	 * 
	 * @throws com.amazonaws.util.json.JSONException
	 * @throws IOException
	 */
	private SpeechletResponse getNewCheckFlightResponse(Slot Airline, Slot FlightNum)
			throws IOException, com.amazonaws.util.json.JSONException {
		
		String speechText = null;

		String arrivalTime = getArrivalTime(Airline.getValue(), FlightNum.getValue());
		
		
		if(arrivalTime == null)
			speechText = "Airline or flight does not exist";
		else
			speechText = "Your flight is scheduled to arrive at " + arrivalTime + " local time";

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
	 * Returns a response for the helps intent.
	 */
	private SpeechletResponse getHelpResponse() {
		String speechText = "Please ask Flight Check the status of a flight by providing the airline name followed by the flight number. For example, Flight Check, what is the status of Lufthansa flight 112. Now, what can I help you with?";
		// Create the plain text output.
		PlainTextOutputSpeech speech = new PlainTextOutputSpeech();
		speech.setText(speechText);

		// Create reprompt
		Reprompt reprompt = new Reprompt();
		reprompt.setOutputSpeech(speech);

		return SpeechletResponse.newAskResponse(speech, reprompt);
	}

	public String getArrivalTime(String Airline, String flightNum)
			throws IOException, com.amazonaws.util.json.JSONException {

		String airlineCode = null;
		URL url1 = new URL("https://iatacodes.org/api/v6/airlines?api_key=9661f1fe-2d46-490c-b475-161c41dd432d");
		URLConnection uc = url1.openConnection();
		uc.connect();
		StringBuilder JSON_DATA = new StringBuilder();
		BufferedReader br = new BufferedReader(new InputStreamReader(uc.getInputStream()));
		String l = null;
		while ((l = br.readLine()) != null) {
			JSON_DATA = JSON_DATA.append(l);
		}
		// br.close();
		System.out.println(JSON_DATA);
		com.amazonaws.util.json.JSONObject obj = new com.amazonaws.util.json.JSONObject(JSON_DATA.toString());
		JSONArray response = obj.getJSONArray("response");
		for (int i = 0; i < response.length(); i++) {
			JSONObject airline = response.getJSONObject(i);
			if (airline.getString("name").equalsIgnoreCase(Airline)) {
				airlineCode = airline.getString("code");
				System.out.println(airlineCode);
			}

		}
		if(airlineCode == null)
			return null;
		String userPassword = "swarooprao:baf818d30d41d7670790c4112afd40501b4329fd";
		String encoding = new String(Base64.encodeBase64(userPassword.getBytes()));

		URL url0 = new URL(
				"http://flightxml.flightaware.com/json/FlightXML2/InFlightInfo?ident=" + airlineCode + flightNum);
		uc = url0.openConnection();
		uc.setRequestProperty("Authorization", "Basic " + encoding);
		uc.connect();
		JSON_DATA = new StringBuilder();
		br = new BufferedReader(new InputStreamReader(uc.getInputStream()));
		l = null;
		while ((l = br.readLine()) != null) {
			JSON_DATA = JSON_DATA.append(l);
		}
		// br.close();
		System.out.println(JSON_DATA);
		obj = new com.amazonaws.util.json.JSONObject(JSON_DATA.toString());
		String faFlightID = obj.getJSONObject("InFlightInfoResult").getString("faFlightID");
		String airportID = obj.getJSONObject("InFlightInfoResult").getString("destination");
		
		if(faFlightID ==null || airportID == null)
			return null;

		URL url2 = new URL("http://flightxml.flightaware.com/json/FlightXML2/AirportInfo?airportCode=" + airportID);
		uc = url2.openConnection();
		uc.setRequestProperty("Authorization", "Basic " + encoding);
		uc.connect();
		JSON_DATA = new StringBuilder();
		br = new BufferedReader(new InputStreamReader(uc.getInputStream()));
		l = null;
		while ((l = br.readLine()) != null) {
			JSON_DATA = JSON_DATA.append(l);
		}
		// 1482572033
		System.out.println(JSON_DATA);
		obj = new com.amazonaws.util.json.JSONObject(JSON_DATA.toString());
		String timezone = obj.getJSONObject("AirportInfoResult").getString("timezone");
		System.out.println(timezone);
		
		if(timezone==null)
			return null;

		URL url = new URL("http://flightxml.flightaware.com/json/FlightXML2/FlightInfoEx?ident=" + faFlightID);
		uc = url.openConnection();
		uc.setRequestProperty("Authorization", "Basic " + encoding);
		uc.connect();
		JSON_DATA = new StringBuilder();
		br = new BufferedReader(new InputStreamReader(uc.getInputStream()));
		l = null;
		while ((l = br.readLine()) != null) {
			JSON_DATA = JSON_DATA.append(l);
		}
		br.close();
		// 1482572033
		System.out.println(JSON_DATA);
		obj = new com.amazonaws.util.json.JSONObject(JSON_DATA.toString());
		String a = obj.getJSONObject("FlightInfoExResult").getJSONArray("flights").getJSONObject(0)
				.getString("estimatedarrivaltime");
		System.out.println(a);
		if(a==null)
			return null;
		Date date = new Date(Long.parseLong(a) * 1000);
		// SimpleDateFormat formatter = new SimpleDateFormat("MM/dd/yyyy");

		SimpleDateFormat formatter = new SimpleDateFormat("hh:mm a");
		formatter.setTimeZone(TimeZone.getTimeZone(timezone.substring(1)));
		String time = formatter.format(date);
		return time;
	}
}
