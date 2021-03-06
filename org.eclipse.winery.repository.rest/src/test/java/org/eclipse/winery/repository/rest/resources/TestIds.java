/*******************************************************************************
 * Copyright (c) 2017 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0, or the Apache Software License 2.0
 * which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 *******************************************************************************/
package org.eclipse.winery.repository.rest.resources;

import org.eclipse.winery.model.ids.Namespace;
import org.eclipse.winery.model.ids.XmlId;
import org.eclipse.winery.model.ids.definitions.NodeTypeId;

public class TestIds {

    public static final Namespace NS = new Namespace("http://winery.opentosca.org/test/nodetypes", false);

    public static final Namespace NS_TEST_FRUITS = new Namespace("http://winery.opentosca.org/test/nodetypes/fruits", false);

    public static final NodeTypeId ID_FRUIT_BAOBAB = new NodeTypeId(NS_TEST_FRUITS, new XmlId("baobab", false));

}
