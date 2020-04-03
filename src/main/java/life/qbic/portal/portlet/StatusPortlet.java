package life.qbic.portal.portlet;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaadin.annotations.Theme;
import com.vaadin.annotations.Widgetset;


import com.vaadin.data.Item;
import com.vaadin.data.sort.Sort;

import com.vaadin.server.Page;
import com.vaadin.server.VaadinRequest;
import com.vaadin.shared.data.sort.SortDirection;
import com.vaadin.ui.*;

import com.vaadin.ui.renderers.DateRenderer;

import life.qbic.datamodel.samples.Location;
import life.qbic.datamodel.samples.Status;
import life.qbic.datamodel.samples.Sample;
import life.qbic.datamodel.samples.SampleSummary;
import life.qbic.datamodel.people.Contact;




//import life.qbic.datamodel.services.Contact;
//import life.qbic.datamodel.services.Location;
//import life.qbic.datamodel.services.Status;
//import life.qbic.datamodel.services.Sample;


import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.apache.commons.codec.binary.Base64;
import java.nio.charset.StandardCharsets;


import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.ArrayList;

import java.text.SimpleDateFormat;

///////////////////////////////

import life.qbic.portal.utils.ConfigurationManagerFactory;
import life.qbic.portal.utils.ConfigurationManager;
import life.qbic.services.ConsulServiceFactory;
import life.qbic.services.Service;
import life.qbic.services.ServiceConnector;
import life.qbic.services.ServiceType;
import life.qbic.services.connectors.ConsulConnector;

import life.qbic.datamodel.services.ServiceUser;




/**
 * Entry point for portlet sample-tracking-status-portlet. This class derives from {@link QBiCPortletUI}, which is found in the {@code portal-utils-lib} library.
 * 
 * @see <a href=https://github.com/qbicsoftware/portal-utils-lib>portal-utils-lib</a>
 */
@Theme("mytheme")
@SuppressWarnings("serial")
@Widgetset("life.qbic.portal.portlet.AppWidgetSet")
public class StatusPortlet extends QBiCPortletUI {

    private static final Logger LOG = LogManager.getLogger(StatusPortlet.class);

    private static List<Service> serviceList;

    private static ServiceUser httpUser;

    @Override
    protected Layout getPortletContent(final VaadinRequest request) {
        LOG.info("Generating content for {}", StatusPortlet.class);

        /////////////
        //set service factory

        ConfigurationManager confManager = ConfigurationManagerFactory.getInstance();
        URL serviceURL = null;

        //System.out.println("$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$ " + confManager.getServicesRegistryUrl());
        //System.out.println("-----> " + confManager.getServiceUser().name);
        //System.out.println("-----> " + confManager.getServiceUser().password);

        serviceList = new ArrayList<Service>();

        httpUser = confManager.getServiceUser();


        try {
            serviceURL = new URL(confManager.getServicesRegistryUrl());


            ServiceConnector connector = new ConsulConnector(serviceURL);
            ConsulServiceFactory factory = new ConsulServiceFactory(connector);
            serviceList.addAll(factory.getServicesOfType(ServiceType.SAMPLE_TRACKING));

//            System.out.println("++++++++++++++++++++++++++++++++++");
//            System.out.println(serviceList.get(0).getRootUrl().toString());
//            System.out.println(serviceList.get(0).getRootUrl().getPath());
//            System.out.println(serviceList.get(0).getRootUrl().getHost());
//            System.out.println(serviceList.get(0).getRootUrl().getPort());
//            System.out.println("++++++++++++++++++++++++++++++++++");


        } catch (MalformedURLException e) {
            //e.printStackTrace();

            Notification notif = new Notification("Error: Backend connection failure","", Notification.TYPE_ERROR_MESSAGE);
            notif.setDelayMsec(20000);
            notif.setPosition(Notification.POSITION_CENTERED_TOP);
            notif.show(Page.getCurrent());
        }


        ////////////

        final HorizontalLayout mainLayout = new HorizontalLayout();
        mainLayout.setSpacing(true);
        mainLayout.setMargin(true);
        mainLayout.setSizeFull();

        mainLayout.addComponent(this.getQueryInterface());

        return mainLayout;

    }

    private Panel getQueryInterface() {

        Panel queryPanel = new Panel("qTracker: sample status check");
        queryPanel.setWidth("100%");

        VerticalLayout panelContent = new VerticalLayout();
        panelContent.setSpacing(true);
        panelContent.setMargin(true);

        //HorizontalLayout inputLayout = new HorizontalLayout();
        //inputLayout.setSpacing(true);
        //inputLayout.setMargin(true);

        TextField idField = new TextField("Enter QBiC ID:");
        idField.setValue("QABCD001A0");

        idField.setWidth("20%");
        Button trackButton = new Button("Find sample");
        trackButton.setWidth("20%");

        Grid logTable = new Grid("Location log:");
        logTable.setSelectionMode(Grid.SelectionMode.NONE);

        //logTable.setWidth("100%");
        logTable.setSizeFull();

        //logTable.addColumn("Date", Date.class);

        logTable.addColumn("Date/Time", Date.class);
        logTable.getColumn("Date/Time").setRenderer(new DateRenderer(new SimpleDateFormat("dd.MM.yyyy '/' HH:mm")));
        logTable.addColumn("Place", String.class);
        logTable.addColumn("Status", String.class);
        logTable.addColumn("Responsible Person", String.class);
        logTable.addColumn("Contact", String.class);

        ////////////////////////////////////////////////////

        panelContent.addComponent(idField);
        panelContent.addComponent(trackButton);

        //panelContent.addComponent(inputLayout);
        panelContent.addComponent(logTable);


        queryPanel.setContent(panelContent);

        trackButton.addClickListener(new Button.ClickListener() {
            public void buttonClick(Button.ClickEvent event) {
                //Notification.show("trying API");
                //testAPIRequest();

                logTable.getContainerDataSource().removeAllItems();

                ////////////////////////////////////////////

                if (idField.getValue().equals("")){

                    logTable.getContainerDataSource().removeAllItems();
                }else {

                    try {

                        //String baseURL = "http://services.qbic.uni-tuebingen.de:8080/sampletrackingservice/";
                        if(serviceList.size() == 0){
                            throw new Exception("No available backend services");
                        }

                        String baseURL = serviceList.get(0).getRootUrl().toString() + "/";

                        ////////////////////////////
                        //auth

                        byte[] credentials = Base64.encodeBase64((httpUser.name + ":" + httpUser.password).getBytes(StandardCharsets.UTF_8));
                        String authHeader = "Basic " + new String(credentials, StandardCharsets.UTF_8);

                        ///////////////////////////

                        HttpClient client = HttpClientBuilder.create().build();

                        String sampleId = idField.getValue();

                        //System.out.println("--->url: " + baseURL + "samples/" + sampleId);

                        HttpGet getSampleInfo = new HttpGet(baseURL + "samples/" + sampleId);
                        getSampleInfo.setHeader("Authorization", authHeader);
                        HttpResponse response = client.execute(getSampleInfo);

                        //System.out.println("--->statusCode: " + String.valueOf(response.getStatusLine().getStatusCode()));

                        if(response.getStatusLine().getStatusCode() == 400){
                            throw new Exception("Invalid ID");
                        }

                        if(response.getStatusLine().getStatusCode() == 404){
                            throw new Exception("ID not found in tracking database");
                        }

                        ////////////////////////////////////////////////////////

                        ObjectMapper mapper = new ObjectMapper();
                        Sample sample = mapper.readValue(response.getEntity().getContent(), Sample.class);
                        //System.out.println(sample);

                        //Date date = new SimpleDateFormat("yyyy-MM-dd").parse(sample.getCurrentLocation().getArrivalDate().substring(0, 10));
                        Date date = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'").parse(sample.getCurrentLocation().getArrivalDate());


                        //logTable.addRow(sample.getCurrentLocation().getArrivalDate().substring(0, 10),
                        logTable.addRow(date,
                                sample.getCurrentLocation().getName(),
                                sample.getCurrentLocation().getStatus().toString(),
                                sample.getCurrentLocation().getResponsiblePerson(),
                                sample.getCurrentLocation().getResponsibleEmail());


                        ///////////////////////////////past locs
                        List<Location> pastLocationList = sample.getpastLocations();
                        //System.out.println(pastLocationList);

                        if(pastLocationList != null){

                            for(Location pastLoc: pastLocationList){

                                //date = new SimpleDateFormat("yyyy-MM-dd").parse(pastLoc.getArrivalDate().substring(0, 10));
                                date = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'").parse(pastLoc.getArrivalDate());

                                //logTable.addRow(pastLoc.getArrivalDate().substring(0, 10),
                                logTable.addRow(date,
                                        pastLoc.getName(),
                                        pastLoc.getStatus().toString(),
                                        pastLoc.getResponsiblePerson(),
                                        pastLoc.getResponsibleEmail());

                            }
                        }


                        logTable.sort(Sort.by("Date/Time", SortDirection.DESCENDING));

                    } catch (Exception E) { //IOException

                        //System.out.println("error********");
                        //E.printStackTrace();

                        /////////////////
                        Notification notif = new Notification("Error: " + E.getMessage(),"", Notification.TYPE_ERROR_MESSAGE);
                        notif.setDelayMsec(20000);
                        notif.setPosition(Notification.POSITION_CENTERED_TOP);
                        notif.show(Page.getCurrent());

                    }

                }

            }
        });

        return queryPanel;
    }
}
