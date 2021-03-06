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
YUI.add("cookie",function(c){var k=c.Lang,i=c.Object,g=null,d=k.isString,n=k.isObject,f=k.isUndefined,e=k.isFunction,h=encodeURIComponent,b=decodeURIComponent,m=c.config.doc;function j(o){throw new TypeError(o);}function l(o){if(!d(o)||o===""){j("Cookie name must be a non-empty string.");}}function a(o){if(!d(o)||o===""){j("Subcookie name must be a non-empty string.");}}c.Cookie={_createCookieString:function(q,t,r,p){p=p||{};var v=h(q)+"="+(r?h(t):t),o=p.expires,u=p.path,s=p.domain;if(n(p)){if(o instanceof Date){v+="; expires="+o.toUTCString();}if(d(u)&&u!==""){v+="; path="+u;}if(d(s)&&s!==""){v+="; domain="+s;}if(p.secure===true){v+="; secure";}}return v;},_createCookieHashString:function(o){if(!n(o)){j("Cookie._createCookieHashString(): Argument must be an object.");}var p=[];i.each(o,function(r,q){if(!e(r)&&!f(r)){p.push(h(q)+"="+h(String(r)));}});return p.join("&");},_parseCookieHash:function(s){var r=s.split("&"),t=g,q={};if(s.length){for(var p=0,o=r.length;p<o;p++){t=r[p].split("=");q[b(t[0])]=b(t[1]);}}return q;},_parseCookieString:function(w,y){var x={};if(d(w)&&w.length>0){var o=(y===false?function(z){return z;}:b),u=w.split(/;\s/g),v=g,p=g,r=g;for(var q=0,s=u.length;q<s;q++){r=u[q].match(/([^=]+)=/i);if(r instanceof Array){try{v=b(r[1]);p=o(u[q].substring(r[1].length+1));}catch(t){}}else{v=b(u[q]);p="";}x[v]=p;}}return x;},_setDoc:function(o){m=o;},exists:function(o){l(o);var p=this._parseCookieString(m.cookie,true);return p.hasOwnProperty(o);},get:function(p,o){l(p);var s,q,r;if(e(o)){r=o;o={};}else{if(n(o)){r=o.converter;}else{o={};}}s=this._parseCookieString(m.cookie,!o.raw);q=s[p];if(f(q)){return g;}if(!e(r)){return q;}else{return r(q);}},getSub:function(o,q,p){var r=this.getSubs(o);if(r!==g){a(q);if(f(r[q])){return g;}if(!e(p)){return r[q];}else{return p(r[q]);}}else{return g;}},getSubs:function(o){l(o);var p=this._parseCookieString(m.cookie,false);if(d(p[o])){return this._parseCookieHash(p[o]);}return g;},remove:function(p,o){l(p);o=c.merge(o||{},{expires:new Date(0)});return this.set(p,"",o);},removeSub:function(p,s,o){l(p);a(s);o=o||{};var r=this.getSubs(p);if(n(r)&&r.hasOwnProperty(s)){delete r[s];if(!o.removeIfEmpty){return this.setSubs(p,r,o);}else{for(var q in r){if(r.hasOwnProperty(q)&&!e(r[q])&&!f(r[q])){return this.setSubs(p,r,o);}}return this.remove(p,o);}}else{return"";}},set:function(p,q,o){l(p);if(f(q)){j("Cookie.set(): Value cannot be undefined.");}o=o||{};var r=this._createCookieString(p,q,!o.raw,o);m.cookie=r;return r;},setSub:function(p,r,q,o){l(p);a(r);if(f(q)){j("Cookie.setSub(): Subcookie value cannot be undefined.");}var s=this.getSubs(p);if(!n(s)){s={};}s[r]=q;return this.setSubs(p,s,o);},setSubs:function(p,q,o){l(p);if(!n(q)){j("Cookie.setSubs(): Cookie value must be an object.");}var r=this._createCookieString(p,this._createCookieHashString(q),false,o);m.cookie=r;return r;}};},"3.4.1",{requires:["yui-base"]});