/*
 * Copyright 2022 Bytedance Inc.
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytedance.primus.webapp;

import com.bytedance.primus.am.AMContext;
import com.bytedance.primus.api.records.Task;
import com.bytedance.primus.utils.ProtoJsonConverter;
import com.bytedance.primus.webapp.bundles.RoleBundle;
import com.bytedance.primus.webapp.bundles.StatusBundle;
import com.bytedance.primus.webapp.bundles.SummaryBundle;
import com.bytedance.primus.webapp.bundles.TaskBundle;
import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.GZIPOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StatusServlet extends HttpServlet {

  private static final Logger LOG = LoggerFactory.getLogger(StatusServlet.class);

  private static AMContext context;
  private static String primusConfJsonString;

  public static void setContext(AMContext context) {
    StatusServlet.context = context;
    StatusServlet.primusConfJsonString = ProtoJsonConverter
        .getJsonString(context.getApplicationMeta().getPrimusConf());
  }

  public static StatusBundle makeStatusBundle() {
    SummaryBundle summaryBundle = SummaryBundle.newBundle(context);
    return new StatusBundle(
        context,
        primusConfJsonString,
        summaryBundle,
        RoleBundle.newBundles(context, summaryBundle),
        // TODO: Better primus-ui to incrementally fetch all tasks
        TaskBundle.newTaskBundles(context, true /* pruneSucceededTasks */)
    );
  }

  public static String buildTaskUri(Task task) {
    if (task.getFileTask() != null) {
      return task.getFileTask().getPath()
          + "[start="
          + task.getFileTask().getStart()
          + ", length="
          + task.getFileTask().getLength()
          + "]";
    } else {
      return "";
    }
  }

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    resp.setContentType("application/json");
    resp.setHeader("Content-Encoding", "gzip");
    tryCompressRes(context.getHdfsStore().getStatusBundleJsonBytes(), resp.getOutputStream());
  }

  public static void tryCompressRes(byte[] res, OutputStream outputStream) throws IOException {
    try (GZIPOutputStream gos = new GZIPOutputStream(outputStream)) {
      gos.write(res);
    }
  }
}
