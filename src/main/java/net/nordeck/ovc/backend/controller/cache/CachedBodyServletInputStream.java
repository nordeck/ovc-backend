package net.nordeck.ovc.backend.controller.cache;

/*
 * Copyright 2025 Nordeck IT + Consulting GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import lombok.SneakyThrows;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

public class CachedBodyServletInputStream extends ServletInputStream
{
    private final InputStream cachedBodyInputStream;

    public CachedBodyServletInputStream(byte[] cachedBody)
    {
        this.cachedBodyInputStream = new ByteArrayInputStream(cachedBody);
    }

    @Override
    @SneakyThrows
    public boolean isFinished()
    {
        return cachedBodyInputStream.available() == 0;
    }

    @Override
    public boolean isReady()
    {
        return true;
    }

    @Override
    public void setReadListener(ReadListener readListener)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public int read() throws IOException
    {
        return cachedBodyInputStream.read();
    }
}
