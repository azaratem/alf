/*
 * Copyright (C) 2005-2012 Alfresco Software Limited.
 *
 * This file is part of Alfresco
 *
 * Alfresco is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Alfresco is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Alfresco. If not, see <http://www.gnu.org/licenses/>.
 */

package org.alfresco.repo.cluster;

import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.util.Collections;

import org.alfresco.repo.jgroups.AlfrescoJGroupsChannelFactory;
import org.alfresco.util.ApplicationContextHelper;
import org.jgroups.Channel;
import org.jgroups.ChannelException;
import org.jgroups.JChannel;
import org.jgroups.Message;
import org.jgroups.ReceiverAdapter;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.context.ApplicationContext;

/**
 * Tests for the JGroups messaging abstractions.
 * 
 * @author Matt Ward
 */
public class JGroupsTest extends ReceiverAdapter
{
    private static ApplicationContext ctx;
    private MessengerTestHelper helper;
    
    @BeforeClass
    public static void setUpClass()
    {
        ctx = ApplicationContextHelper.
                getApplicationContext(new String[] { "cluster-test/jgroups-messenger-test.xml" });
    }
    
    @AfterClass
    public static void tearDownClass()
    {
        ApplicationContextHelper.closeApplicationContext();
    }
    
    @Before
    public void setUp()
    {
        helper = new MessengerTestHelper();
    }
 
    @Test
    public void canSendWithJGroupsMessengerFactory() throws InterruptedException, ChannelException
    {
        Channel ch = new JChannel("udp.xml");
        ch.connect("testcluster:testregion");
        ch.setReceiver(this);
        
        MessengerFactory messengerFactory = (MessengerFactory) ctx.getBean("messengerFactory");
        Messenger<String> messenger = messengerFactory.createMessenger();
        messenger.send("Full test including spring.");
        
        helper.checkMessageReceivedWas("Full test including spring.");
    }
    
    @Test
    public void canSendWithJGroupsMessengerFactoryWithoutSpring() throws InterruptedException, ChannelException
    {
        Channel ch = new JChannel("udp.xml");
        ch.connect("testcluster:testregion");
        ch.setReceiver(this);
        
        AlfrescoJGroupsChannelFactory channelFactory = new AlfrescoJGroupsChannelFactory();
        channelFactory.setClusterName("testcluster");
        channelFactory.setConfigUrlsByAppRegion(Collections.singletonMap("DEFAULT", "classpath:udp.xml"));
        AlfrescoJGroupsChannelFactory.rebuildChannels();
        
        JGroupsMessengerFactory messengerFactory = new JGroupsMessengerFactory();
        messengerFactory.setAppRegion("testregion");
        Messenger<String> messenger = messengerFactory.createMessenger();
        messenger.send("This is a test payload.");
        
        helper.checkMessageReceivedWas("This is a test payload.");
    }
    
    @Test
    public void canWrapRawChannels() throws ChannelException, InterruptedException
    {
        Channel sendCh = new JChannel("udp.xml");
        sendCh.connect("mycluster");
        Messenger<String> messenger = new JGroupsMessenger<String>(sendCh);

        Channel recvCh = new JChannel("udp.xml");
        recvCh.connect("mycluster");
        recvCh.setReceiver(this);
        
        messenger.send("This message was sent with jgroups");
        
        helper.checkMessageReceivedWas("This message was sent with jgroups");
    }
    
    @Ignore("Not currently allowing multiple receivers per underlying Channel")
    @Test
    public void canReceiveFromMultipleMessageReceivers() throws InterruptedException, ChannelException
    {
        Channel ch = new JChannel("udp.xml");
        ch.connect("mycluster");
        Messenger<String> sendMsgr = new JGroupsMessenger<String>(ch);

        Messenger<String> recvMsgr1 = new JGroupsMessenger<String>(ch);
        TestMessageReceiver r1 = new TestMessageReceiver();
        recvMsgr1.setReceiver(r1);
        
        Messenger<String> recvMsgr2 = new JGroupsMessenger<String>(ch);
        TestMessageReceiver r2 = new TestMessageReceiver();
        recvMsgr2.setReceiver(r2);
        
        sendMsgr.send("This message was sent with jgroups");
        
        Thread.sleep(50);
        assertEquals("This message was sent with jgroups", new String(r1.receivedMsg));
        assertEquals("This message was sent with jgroups", new String(r2.receivedMsg));
    }
    
    @Override
    public void receive(Message msg)
    {
        ByteArrayInputStream bytes = new ByteArrayInputStream(msg.getBuffer());
        ObjectInput in;
        try
        {
            in = new ObjectInputStream(bytes);
            String payload = (String) in.readObject();
            in.close();
            bytes.close();
            helper.setReceivedMsg(payload);
        }
        catch (IOException e)
        {
            throw new RuntimeException("Couldn't receive object.", e);
        }
        catch (ClassNotFoundException e)
        {
            throw new RuntimeException("Couldn't receive object.", e);
        }
    }
    
    
    public static class TestMessageReceiver implements MessageReceiver<String>
    {
        String receivedMsg;
        
        @Override
        public void onReceive(String message)
        {
            receivedMsg = message;
        }   
    }
}