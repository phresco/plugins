/*
 * Phresco Maven Plugin
 *
 * Copyright (C) 1999-2014 Photon Infotech Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/*
YUI 3.4.1 (build 4118)
Copyright 2011 Yahoo! Inc. All rights reserved.
Licensed under the BSD License.
http://yuilibrary.com/license/
*/
YUI.add('datasource-arrayschema', function(Y) {

/**
 * Extends DataSource with schema-parsing on array data.
 *
 * @module datasource
 * @submodule datasource-arrayschema
 */

/**
 * Adds schema-parsing to the DataSource Utility.
 * @class DataSourceArraySchema
 * @extends Plugin.Base
 */    
var DataSourceArraySchema = function() {
    DataSourceArraySchema.superclass.constructor.apply(this, arguments);
};

Y.mix(DataSourceArraySchema, {
    /**
     * The namespace for the plugin. This will be the property on the host which
     * references the plugin instance.
     *
     * @property NS
     * @type String
     * @static
     * @final
     * @value "schema"
     */
    NS: "schema",

    /**
     * Class name.
     *
     * @property NAME
     * @type String
     * @static
     * @final
     * @value "dataSourceArraySchema"
     */
    NAME: "dataSourceArraySchema",

    /////////////////////////////////////////////////////////////////////////////
    //
    // DataSourceArraySchema Attributes
    //
    /////////////////////////////////////////////////////////////////////////////

    ATTRS: {
        schema: {
            //value: {}
        }
    }
});

Y.extend(DataSourceArraySchema, Y.Plugin.Base, {
    /**
    * Internal init() handler.
    *
    * @method initializer
    * @param config {Object} Config object.
    * @private
    */
    initializer: function(config) {
        this.doBefore("_defDataFn", this._beforeDefDataFn);
    },

    /**
     * Parses raw data into a normalized response.
     *
     * @method _beforeDefDataFn
     * @param tId {Number} Unique transaction ID.
     * @param request {Object} The request.
     * @param callback {Object} The callback object with the following properties:
     *     <dl>
     *         <dt>success (Function)</dt> <dd>Success handler.</dd>
     *         <dt>failure (Function)</dt> <dd>Failure handler.</dd>
     *     </dl>
     * @param data {Object} Raw data.
     * @protected
     */
    _beforeDefDataFn: function(e) {
        var data = (Y.DataSource.IO && (this.get("host") instanceof Y.DataSource.IO) && Y.Lang.isString(e.data.responseText)) ? e.data.responseText : e.data,
            response = Y.DataSchema.Array.apply.call(this, this.get("schema"), data),
            payload = e.details[0];
            
        // Default
        if (!response) {
            response = {
                meta: {},
                results: data
            };
        }
        
        payload.response = response;

        this.get("host").fire("response", payload);

        return new Y.Do.Halt("DataSourceArraySchema plugin halted _defDataFn");
    }
});
    
Y.namespace('Plugin').DataSourceArraySchema = DataSourceArraySchema;


}, '3.4.1' ,{requires:['datasource-local', 'plugin', 'dataschema-array']});
