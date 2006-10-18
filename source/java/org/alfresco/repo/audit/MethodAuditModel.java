/*
 * Copyright (C) 2005 Alfresco, Inc.
 *
 * Licensed under the Mozilla Public License version 1.1 
 * with a permitted attribution clause. You may obtain a
 * copy of the License at
 *
 *   http://www.alfresco.org/legal/license.txt
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific
 * language governing permissions and limitations under the
 * License.
 */
package org.alfresco.repo.audit;

import org.alfresco.repo.audit.model.TrueFalseUnset;
import org.aopalliance.intercept.MethodInvocation;

public interface MethodAuditModel
{
    /**
     * Report if audit behaviour can be determined before the method call
     * 
     * @param auditState,
     * @param mi
     * @return
     */
    public AuditMode beforeExecution(AuditMode auditMode, MethodInvocation mi);

    /**
     * Report if audit behaviour can be determined after the method call
     * 
     * @param auditState,
     * @param mi
     * @return
     */
    public AuditMode afterExecution(AuditMode auditMode, MethodInvocation mi);

    /**
     * Report if audit behaviour should be invoked on error. It could be we look at the error and filter - this is not supported at the moment.
     * 
     * @param auditState,
     * @param mi
     * @return
     */
    public AuditMode onError(AuditMode auditMode, MethodInvocation mi);

    /**
     * Get the optional parameters that are to be recorded
     * 
     * @param mi
     * @return
     */
    public RecordOptions getAuditRecordOptions(MethodInvocation mi);
    
    /**
     * Should internal service class be logged.
     * 
     * @return
     */
    public TrueFalseUnset getAuditInternalServiceMethods(MethodInvocation mi);
}
