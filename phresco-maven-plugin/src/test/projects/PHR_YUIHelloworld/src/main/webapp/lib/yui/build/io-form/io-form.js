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
YUI.add('io-form', function(Y) {

/**
* Extends IO to enable HTML form data serialization, when specified
* in the transaction's configuration object.
* @module io-base
* @submodule io-form
* @for IO
*/

var eUC = encodeURIComponent;

Y.mix(Y.IO.prototype, {
   /**
    * Method to enumerate through an HTML form's elements collection
    * and return a string comprised of key-value pairs.
    *
    * @method _serialize
    * @private
    * @static
    * @param {Object} c - YUI form node or HTML form id.
    * @param {String} s - Key-value data defined in the configuration object.
    * @return {String}
    */
    _serialize: function(c, s) {
        var data = [],
            df = c.useDisabled || false,
            item = 0,
            id = (typeof c.id === 'string') ? c.id : c.id.getAttribute('id'),
            e, f, n, v, d, i, il, j, jl, o;

            if (!id) {
                id = Y.guid('io:');
                c.id.setAttribute('id', id);
            }

            f = Y.config.doc.getElementById(id);

        // Iterate over the form elements collection to construct the
        // label-value pairs.
        for (i = 0, il = f.elements.length; i < il; ++i) {
            e = f.elements[i];
            d = e.disabled;
            n = e.name;

            if (df ? n : n && !d) {
                n = eUC(n) + '=';
                v = eUC(e.value);

                switch (e.type) {
                    // Safari, Opera, FF all default options.value from .text if
                    // value attribute not specified in markup
                    case 'select-one':
                        if (e.selectedIndex > -1) {
                            o = e.options[e.selectedIndex];
                            data[item++] = n + eUC(o.attributes.value && o.attributes.value.specified ? o.value : o.text);
                        }
                        break;
                    case 'select-multiple':
                        if (e.selectedIndex > -1) {
                            for (j = e.selectedIndex, jl = e.options.length; j < jl; ++j) {
                                o = e.options[j];
                                if (o.selected) {
                                  data[item++] = n + eUC(o.attributes.value && o.attributes.value.specified ? o.value : o.text);
                                }
                            }
                        }
                        break;
                    case 'radio':
                    case 'checkbox':
                        if (e.checked) {
                            data[item++] = n + v;
                        }
                        break;
                    case 'file':
                        // stub case as XMLHttpRequest will only send the file path as a string.
                    case undefined:
                        // stub case for fieldset element which returns undefined.
                    case 'reset':
                        // stub case for input type reset button.
                    case 'button':
                        // stub case for input type button elements.
                        break;
                    case 'submit':
                    default:
                        data[item++] = n + v;
                }
            }
        }
        return s ? data.join('&') + "&" + s : data.join('&');
    }
}, true);


}, '3.4.1' ,{requires:['io-base','node-base']});
