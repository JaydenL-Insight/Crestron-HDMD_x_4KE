package com.insightsystems.dal.crestron;

import com.avispl.symphony.api.dal.control.Controller;
import com.avispl.symphony.api.dal.dto.control.AdvancedControllableProperty;
import com.avispl.symphony.api.dal.dto.control.ControllableProperty;
import com.avispl.symphony.api.dal.dto.monitor.ExtendedStatistics;
import com.avispl.symphony.api.dal.dto.monitor.Statistics;
import com.avispl.symphony.api.dal.monitor.Monitorable;
import com.avispl.symphony.api.dal.ping.Pingable;
import com.avispl.symphony.dal.communicator.HttpCommunicator;
import com.avispl.symphony.dal.communicator.TelnetCommunicator;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HDMD_x_4KE extends TelnetCommunicator implements Controller, Pingable, Monitorable {
    private final String CRLF = "\r\n";
    private int NUM_INPUTS = 4,NUM_OUTPUTS = 1;
    private final String queryInputName = "show input ' name",queryInputHDCPActive = "show input ' video hdcp active", queryInputHDCPSupport = "show input ' video hdcp support",queryInputHDCPState = "show input ' video hdcp state";
    private final String queryInputSyncDetect = "show input ' syncdetect";
    private final String queryOutputName = "show output ' name", queryOutputRoute = "show output ' route", queryOutputHotplugState = "show output ' video hotplug state",queryOutputDisabled = "show output ' video output disabled";
    private final String queryOutputHDCPAlwaysOn = "show output ' video hdcp alwayson",queryOutputCecError = "show output ' video cec error", queryOutputHres = "show output ' video hres",queryOutputVres = "show output ' video vres";
    private final String queryOutputAspectRatio = "show output ' video aspectratio", queryOutputRefreshRate = "show output ' video refreshrate", queryOutputDeepcolorMode = "show output ' video deepcolormode";
    private final String queryOutputColorSpaceMode = "show output ' video colorspacemode", queryOutputHDCPState = "show output ' video hdcp state", queryOutputAutoRoute = "show output autoroute", queryOutputSinkManufacturer = "show output ' sink manufacturer";
    private final String queryOutputSinkName = "show output ' sink name", queryFrontPanelLock = "show frontpanel lock";

    private final String cmdReboot = "reboot",cmdRoute = "conf output <o> route <i>", cmdFrontPanelLock = "conf frontpanel lock <s>", cmdAutoRoute = "conf output autoroute <s>";
    private String[] inputNames;
    private String[] outputNames;

    protected HttpDevice http = new HttpDevice();


    public HDMD_x_4KE(){
        this.setCommandErrorList(Collections.singletonList("BLAH"));
        this.setCommandSuccessList(Collections.singletonList(""));
        this.setLoginErrorList(Collections.singletonList("BLAH"));
        this.setLoginSuccessList(Collections.singletonList(""));
        this.setLogin("");
        this.setPassword("");
        this.setPort(23);
        this.setTimeout(3000); //Device responds quickly or not at all. Incorrect commands will not send a response so best to have short timeout
        http.setPort(80);
        http.setProtocol("http");
        http.setAuthenticationScheme(HttpCommunicator.AuthenticationScheme.None);
        http.setPassword("");
        http.setLogin("");

    }

    @Override
    protected void internalInit() throws Exception {
        super.internalInit();
        http.setHost(this.getHost());
        http.init();
    }

    @Override
    public List<Statistics> getMultipleStatistics() throws Exception {
        ExtendedStatistics extStats = new ExtendedStatistics();
        Map<String,String> stats = new LinkedHashMap<>();
        List<AdvancedControllableProperty> controls;

        send("");
        final String httpResponse = http.get("");
        String model = regexFind(httpResponse,"model='([^']+)'");
        stats.put("Model",model);
        stats.put("SerialNumber",regexFind(httpResponse,"seriv='([^']+)'"));
        stats.put("FirmwareVersion",regexFind(httpResponse,"firmv='([^']+)'"));
        stats.put("HostName",regexFind(httpResponse,"host='([^']+)'"));
        stats.put("MacAddress",regexFind(httpResponse,"mac='([^']+)'"));
        final String[] inOuts = regexFind(model,"HD-MD(\\d+x\\d+)-").split("x");
        if (inOuts.length == 2){ //Dynamically Change number of ins and outs based on model
            NUM_INPUTS  = Integer.parseInt(inOuts[0]);
            NUM_OUTPUTS = Integer.parseInt(inOuts[1]);
        } else {
            NUM_INPUTS = 4;
            NUM_OUTPUTS = 1;
        }

        final String autoRoute = queryDevice(queryOutputAutoRoute,1);
        final String frontPanelLock = queryDevice(queryFrontPanelLock,1);
        stats.put("AutoRoute",autoRoute);
        stats.put("FrontPanelLock",frontPanelLock);

        inputNames = getInputNames();
        outputNames = getOutputNames();


        for (int i = 1; i <= NUM_INPUTS;i++){
            stats.put(inputNames[i-1]+"SyncDetected",queryDevice(queryInputSyncDetect,i));
            stats.put(inputNames[i-1]+"HdcpActive",queryDevice(queryInputHDCPActive,i));
            stats.put(inputNames[i-1]+"HdcpSupport",queryDevice(queryInputHDCPSupport,i));
            stats.put(inputNames[i-1]+"HdcpState",queryDevice(queryInputHDCPState,i));
        }

        int[] outputRoutes = new int[NUM_OUTPUTS];
        for (int i = 1; i <= NUM_OUTPUTS;i++){
            stats.put(outputNames[i-1]+"HotplugState",queryDevice(queryOutputHotplugState,i));
            stats.put(outputNames[i-1]+"OutputDisabled",queryDevice(queryOutputDisabled,i));
            stats.put(outputNames[i-1]+"HdcpAlwaysOn",queryDevice(queryOutputHDCPAlwaysOn,i));
            stats.put(outputNames[i-1]+"CecError",queryDevice(queryOutputCecError,i));
            stats.put(outputNames[i-1]+"AspectRatio",queryDevice(queryOutputAspectRatio,i));
            stats.put(outputNames[i-1]+"DeepcolorMode",queryDevice(queryOutputDeepcolorMode,i));
            stats.put(outputNames[i-1]+"ColorSpaceMode",queryDevice(queryOutputColorSpaceMode,i));
            stats.put(outputNames[i-1]+"HdcpState",queryDevice(queryOutputHDCPState,i));
            stats.put(outputNames[i-1]+"SinkManufacturer",queryDevice(queryOutputSinkManufacturer,i));
            stats.put(outputNames[i-1]+"SinkName",queryDevice(queryOutputSinkName,i));

            final String vres = queryDevice(queryOutputVres,i);
            final String hres = queryDevice(queryOutputHres,i);
            final String rRate = queryDevice(queryOutputRefreshRate,i);
            stats.put(outputNames[i-1]+"Resolution",vres+"x"+hres+"@"+rRate);

            outputRoutes[i-1] = Integer.parseInt(queryDevice(queryOutputRoute,i));
        }



        controls = getControls(autoRoute,frontPanelLock,outputRoutes);
        extStats.setControllableProperties(controls);
        extStats.setStatistics(stats);
        return Collections.singletonList(extStats);
    }

    private List<AdvancedControllableProperty> getControls(String autoRouteState, String frontPanelLockState, int[] outputRoutes) {
        List<AdvancedControllableProperty> controls = new ArrayList<>();

        AdvancedControllableProperty.Button rebootButton = new AdvancedControllableProperty.Button();
        rebootButton.setGracePeriod(10000L);
        rebootButton.setLabelPressed("Rebooting..");
        rebootButton.setLabel("Reboot");
        controls.add(new AdvancedControllableProperty("Reboot",new Date(),rebootButton,"0"));

        AdvancedControllableProperty.Switch frontPanelLockSwitch = new AdvancedControllableProperty.Switch();
        frontPanelLockSwitch.setLabelOff("Disabled");
        frontPanelLockSwitch.setLabelOn("Enabled");
        controls.add(new AdvancedControllableProperty("FrontPanelLock",new Date(),frontPanelLockSwitch,frontPanelLockState));

        AdvancedControllableProperty.Switch autoRouteSwitch = new AdvancedControllableProperty.Switch();
        frontPanelLockSwitch.setLabelOff("Disabled");
        frontPanelLockSwitch.setLabelOn("Enabled");
        controls.add(new AdvancedControllableProperty("AutoRoute",new Date(),autoRouteSwitch,autoRouteState));

        for (int i = 1; i<=NUM_OUTPUTS;i++){
            AdvancedControllableProperty.DropDown routeDropdown = new AdvancedControllableProperty.DropDown();
            routeDropdown.setLabels(inputNames);
            routeDropdown.setOptions(getOptions());
            controls.add(new AdvancedControllableProperty(outputNames[i-1]+"Source",new Date(),routeDropdown,String.valueOf(outputRoutes[i-1])));
        }
        return controls;
    }

    private String[] getOptions() {
        String[] options = new String[NUM_INPUTS];
        for (int i=1;i<=NUM_INPUTS;i++){
            options[i-1] = String.valueOf(i);
        }
        return options;
    }

    private String queryDevice(String command, int index) throws Exception {
        command = command.replace("'",String.valueOf(index));
        return send(command+CRLF).replace(command.replace("show","event"),"").trim();
    }

    private String[] getOutputNames() throws Exception {
        String[] names = new String[NUM_OUTPUTS];
        for (int i = 1; i <= NUM_OUTPUTS; i++){
            String command = queryOutputName.replace("'",String.valueOf(i));
            names[i-1] = send(command+CRLF).replace(command.replace("show","event"),"").trim();
        }
        return names;
    }

    private String[] getInputNames() throws Exception {
        String[] names = new String[NUM_INPUTS];
        for (int i = 1; i <= NUM_INPUTS; i++){
            String command = queryInputName.replace("'",String.valueOf(i));
            names[i-1] = send(command+CRLF).replace(command.replace("show","event"),"").trim();
        }
        return names;
    }

    private String regexFind(String sourceString,String regex){
        final Matcher matcher = Pattern.compile(regex).matcher(sourceString);
        return matcher.find() ? matcher.group(1) : "";
    }

    @Override
    public void controlProperty(ControllableProperty cp) throws Exception {
        if (cp.getProperty().isEmpty() || cp.getProperty() == null)
            return;

        switch (cp.getProperty()){

            case "reboot":
                send(cmdReboot+CRLF);
                //Todo add check for success
                break;

            case "FrontPanelLock":
                send(cmdFrontPanelLock.replace("<s>",cp.getValue().toString().equals("1") ? "true":"false") + CRLF);
                //Todo add check for success
                break;

            case "AutoRoute":
                send(cmdAutoRoute.replace("<s>",cp.getValue().toString().equals("1") ? "true":"false") + CRLF);
                //Todo add check for success
                break;

            default:
                //Also check if it matches the name of any of our output source fields.
                for (int i = 1; i <= NUM_OUTPUTS;i++){
                    if ((cp.getProperty()+"Source").equals(outputNames[i-1]+"Source")){
                        send(cmdRoute.replace("<i>",cp.getValue().toString()).replace("<o>", String.valueOf(i)));
                        //Todo add check for response.
                        return;
                    }
                }
            //If we get here something went wrong. :/
            this.logger.warn("Controllable property: " + cp.getProperty() + " Could not be found.");
        }

    }

    @Override
    public void controlProperties(List<ControllableProperty> list) throws Exception {
        for (ControllableProperty cp : list){
            this.controlProperty(cp);
            Thread.sleep(50L); //Switcher starts to error if commands are sent too quickly
        }
    }

    public static void main(String[] args) throws Exception {
        HDMD_x_4KE device = new HDMD_x_4KE();
        device.setHost("10.215.79.71");
        device.init();
        ((ExtendedStatistics)device.getMultipleStatistics().get(0)).getStatistics().forEach((k,v)-> System.out.println(k.replaceAll("\r","r").replaceAll("\n","n") + " : " + v.replaceAll("\r","r").replaceAll("\n","n")));
    }
}
