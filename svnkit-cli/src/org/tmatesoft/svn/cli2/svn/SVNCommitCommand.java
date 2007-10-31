/*
 * ====================================================================
 * Copyright (c) 2004-2007 TMate Software Ltd.  All rights reserved.
 *
 * This software is licensed as described in the file COPYING, which
 * you should have received as part of this distribution.  The terms
 * are also available at http://svnkit.com/license.html.
 * If newer versions of this license are posted there, you may use a
 * newer version instead, at your option.
 * ====================================================================
 */
package org.tmatesoft.svn.cli2.svn;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.tmatesoft.svn.cli2.SVNCommandTarget;
import org.tmatesoft.svn.core.SVNCommitInfo;
import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.wc.SVNErrorManager;
import org.tmatesoft.svn.core.wc.SVNCommitClient;


/**
 * @version 1.1.2
 * @author  TMate Software Ltd.
 */
public class SVNCommitCommand extends SVNCommand {

    public SVNCommitCommand() {
        super("commit", new String[] {"ci"});
    }
    
    public boolean isCommitter() {
        return true;
    }

    protected Collection createSupportedOptions() {
        Collection options = new LinkedList();
        options.add(SVNOption.QUIET);
        options.add(SVNOption.NON_RECURSIVE);
        options.add(SVNOption.DEPTH);
        options.add(SVNOption.TARGETS);
        options.add(SVNOption.NO_UNLOCK);
        options = SVNOption.addLogMessageOptions(options);
        options = SVNOption.addAuthOptions(options);
        options.add(SVNOption.CHANGELIST);
        options.add(SVNOption.KEEP_CHANGELIST);
        options.add(SVNOption.CONFIG_DIR);
        return options;
    }

    public void run() throws SVNException {
        List targets = getSVNEnvironment().combineTargets(getSVNEnvironment().getTargets());
        if (targets.isEmpty()) {
            targets.add(".");
        }
        SVNDepth depth = getSVNEnvironment().getDepth();
        if (depth == SVNDepth.UNKNOWN) {
            depth = SVNDepth.INFINITY;
        }
        SVNCommitClient client = getSVNEnvironment().getClientManager().getCommitClient();
        Collection filesList = new ArrayList();
        for (Iterator ts = targets.iterator(); ts.hasNext();) {
            String targetName = (String) ts.next();
            SVNCommandTarget target = new SVNCommandTarget(targetName);
            if (target.isFile()) {
                filesList.add(target.getFile());
            } else {
                getSVNEnvironment().getOut().println("Skipped '" + targetName + "'");
            }
        }
        if (filesList.isEmpty()) {
            return;
        }
        File[] files = (File[]) filesList.toArray(new File[filesList.size()]);

        if (!getSVNEnvironment().isQuiet()) {
            client.setEventHandler(new SVNNotifyPrinter(getSVNEnvironment()));
        }        
        client.setCommitHandler(getSVNEnvironment());
        boolean keepLocks = getSVNEnvironment().getClientManager().getOptions().isKeepLocks();
        SVNCommitInfo info = null;
        try {
            info = client.doCommit(files, keepLocks, getSVNEnvironment().getMessage(), 
                    getSVNEnvironment().getRevisionProperties(),
                    getSVNEnvironment().getChangelist(), 
                    getSVNEnvironment().isKeepChangelist(), 
                    getSVNEnvironment().isForce(), depth);
        } catch (SVNException svne) {
            SVNErrorMessage err = svne.getErrorMessage().getRootErrorMessage();
            if (err.getErrorCode() == SVNErrorCode.UNKNOWN_CHANGELIST) {
                SVNErrorManager.error(err);
            }
            throw svne;
        }

        if (!getSVNEnvironment().isQuiet()) {
            getSVNEnvironment().printCommitInfo(info);
        }
    }

}
