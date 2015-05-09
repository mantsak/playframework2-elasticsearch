package com.github.eduardofcbg.plugin.es;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.node.Node;

import play.Application;
import play.Play;
import play.Plugin;
import static org.elasticsearch.node.NodeBuilder.*;


public class ESPlugin extends Plugin {
	
    private final Application application;
    private ObjectMapper mapper;

    private Client client;
    private Node node;

    public ESPlugin(Application application) {
        this.application = application;
        if (enabled()) {
        	setMapper(new ObjectMapper());
            this.mapper.enable(SerializationFeature.INDENT_OUTPUT);
        }
    }
    
    public void setMapper(ObjectMapper mapper) {
    	this.mapper = mapper;
    }

    private final boolean isPluginDisabled() {
        final boolean status =  application.configuration().getBoolean("es.enabled", false);
        return !status;
    }

    public final static ESPlugin getPlugin() {
        return Play.application().plugin(ESPlugin.class);
    }

    public final Client getClient() {
        return client;
    }
    
    public final ObjectMapper getMapper() {
        return mapper;
    }

    @Override
    public final boolean enabled() {
        return !isPluginDisabled();
    }

    @Override
    public final void onStart() {
    	final boolean local = application.configuration().getBoolean("es.embed", false);
    	if (local)
    		createNode();
    	else
    		setTransportClient();
    }

    private void createNode() {
    	node = nodeBuilder().local(true).node();
    	client = node.client();
	}

	private void setTransportClient() {
        ImmutableSettings.Builder settings = ImmutableSettings.settingsBuilder();

    	final String[] hosts = application.configuration().getString("es.client").split(",");
    	final boolean sniff = application.configuration().getBoolean("es.sniff", false);
    	final String pingTimeout = application.configuration().getString("es.timeout");
    	final String pingFrequency = application.configuration().getString("es.ping");
    	final String clusterName = application.configuration().getString("es.cluster");
    	
        if (sniff) settings.put("client.transport.sniff", true);
        if (pingTimeout != null) settings.put("client.transport.ping_timeout", pingTimeout);
        if (pingFrequency != null) settings.put("client.transport.nodes_sampler_interval", pingTimeout);
        if (clusterName != null) settings.put("cluster.name", clusterName);
        
        TransportClient client = new TransportClient(settings.build());
        for(int i = 0; i < hosts.length; i++)
        	client.addTransportAddress(
        			new InetSocketTransportAddress(hosts[i].split(":")[0], Integer.valueOf(hosts[i].split(":")[1])));
        
        this.client = client;
	}

	@Override
    public final void onStop() {
		if (node != null) node.close();
        client.close();
    }

	public final String indexName() {
		return application.configuration().getString("es.index", "play");
	}
	
	
}
