/*
 * Copyright 2000-2009 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.theoryinpractice.testng;

import consulo.ui.ex.awt.MessagesEx;

/**
 * @author Hani Suleiman
 *         Date: Jul 20, 2005
 *         Time: 2:07:53 PM
 */
public class MessageInfoException extends Exception
{
    private final MessagesEx.MessageInfo info;

    public MessageInfoException(MessagesEx.MessageInfo info) {
        this.info = info;
    }

    public MessagesEx.MessageInfo getMessageInfo() {
        return info;
    }
}
