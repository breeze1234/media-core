/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2017, Telestax Inc and individual contributors
 * by the @authors tag. 
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.restcomm.media.rtp.connection;

import org.apache.log4j.Logger;
import org.squirrelframework.foundation.fsm.AnonymousAction;

import com.google.common.util.concurrent.FutureCallback;

/**
 * Notifies listener that RTP Connection is closed.
 * <p>
 * Input parameters:
 * <ul>
 * <li>CALLBACK</li>
 * </ul>
 * </p>
 * <p>
 * Output parameters:
 * <ul>
 * <li>n/a</li>
 * </ul>
 * </p>
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class NotifyClosedAction extends AnonymousAction<RtpConnectionFsm, RtpConnectionState, RtpConnectionEvent, RtpConnectionTransitionContext> {

    private static final Logger log = Logger.getLogger(NotifyClosedAction.class);
    
    static final NotifyClosedAction INSTANCE = new NotifyClosedAction();
    
    NotifyClosedAction() {
        super();
    }

    @Override
    public void execute(RtpConnectionState from, RtpConnectionState to, RtpConnectionEvent event,
            RtpConnectionTransitionContext context, RtpConnectionFsm stateMachine) {
        // Get input parameters
        final FutureCallback<?> callback = context.get(RtpConnectionTransitionParameter.CALLBACK, FutureCallback.class);

        // Warn callback that operation failed
        callback.onSuccess(null);
    }

}