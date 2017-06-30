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

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public enum RtpConnectionEvent {

    OPEN, 
    PARSED_REMOTE_DESCRIPTION,
    PARSE_REMOTE_DESCRIPTION_FAILURE,
    SESSION_ALLOCATED,
    SESSION_ALLOCATION_FAILURE, 
    SESSION_MODE_UPDATED,
    SESSION_MODE_UPDATE_FAILURE, 
    SESSION_NEGOTIATED,
    SESSION_NEGOTIATION_FAILURE,
    GENERATED_LOCAL_DESCRIPTION,
    GENERATE_LOCAL_DESCRIPTION_FAILURE,
    OPENED, CORRUPTED, 
    UPDATE_MODE,
    MODE_UPDATED, 
    CLOSE,
    SESSION_CLOSED,
    SESSION_CLOSE_FAILURE,
    CLOSED;
    
}