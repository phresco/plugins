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
YUI.add('datasource-xmlschema-tests', function(Y) {

var Assert = Y.Assert,

    suite = new Y.Test.Suite("Plugin.DataSourceXMLSchema Test Suite"),

    xmlData; // at end of file because it's huge


suite.add(new Y.Test.Case({
    name: "DataSource XMLSchema Plugin Tests",

    testXMLSchema: function() {
        var ds = new Y.DataSource.Local({ source: xmlData }),
            request = null, response;

        ds.plug(Y.Plugin.DataSourceXMLSchema, {
            schema: {
                resultListLocator: "result",
                resultFields: [{key:"title", locator:"*[local-name() ='title']"}]
            }
        });

        ds.sendRequest({
            callback: {
                success: function (e) {
                    request  = e.request;
                    response = e.response;
                }
            }
        });

        Assert.isUndefined(request, "Expected undefined request.");
        Assert.isObject(response, "Expected normalized response object.");
        Assert.isArray(response.results, "Expected results array.");
        Assert.areSame(10, response.results.length, "Expected 10 results.");
        Assert.isNotUndefined(response.results[0].title, "Expected Title property");
    },

    testSchemaError: function() {
        var ds = new Y.DataSource.Local({ source: xmlData }),
            request = null, response, error;

        ds.plug(Y.Plugin.DataSourceXMLSchema);

        ds.sendRequest({
            callback: {
                failure: function (e) {
                    response = e.response;
                    error    = e.error;
                }
            }
        });

        Assert.isObject(response, "Expected normalized response object.");
        Assert.isObject(error, "Expected response error.");
    }
}));

Y.Test.Runner.add(suite);


/******************************************************************************/
/********************* Big data follows. Nothing to see here. *****************/
/******************************************************************************/
xmlData = Y.DataType.XML.parse('<query xmlns:yahoo="http://www.yahooapis.com/v1/base.rng" yahoo:count="10" yahoo:created="2009-04-30T12:03:12Z" yahoo:lang="en-US" yahoo:updated="2009-04-30T12:03:12Z"><diagnostics><publiclyCallable>true</publiclyCallable><url execution-time="115"><![CDATA[http://boss.yahooapis.com/ysearch/web/v1/madonna?format=xml&start=0&count=10]]></url><user-time>118</user-time><service-time>115</service-time><build-version>1432</build-version></diagnostics><results><result xmlns="http://www.inktomi.com/"><abstract><![CDATA[<b>Madonnas</b> official web site and fan club, featuring news, photos, concert tickets, merchandise, and more. <b>...</b> <b>Madonna</b> HOME. login. join. Raising Malawi. NEWS <b>...</b>]]></abstract><clickurl>http://lrd.yahooapis.com/_ylc=X3oDMTQ4bDQyNGY3BF9TAzIwMjMxNTI3MDIEYXBwaWQDb0pfTWdwbklrWW5CMWhTZnFUZEd5TkouTXNxZlNMQmkEY2xpZW50A2Jvc3MEc2VydmljZQNCT1NTBHNsawN0aXRsZQRzcmNwdmlkA2pTRTJBMGdlQXUxVDcyems0VW5vdWczUTBZTS5jVW40NnNBQUFscVI-/SIG=10splijt0/**http%3A//www.madonna.com/</clickurl><date>2009/04/28</date><dispurl><![CDATA[www.<b>madonna.com</b>]]></dispurl><size>144947</size><title><![CDATA[<b>madonna</b>.com home]]></title><url>http://www.madonna.com/</url></result><result xmlns="http://www.inktomi.com/"><abstract><![CDATA[Exhaustive bio and discography of <b>Madonna\'s</b> early life, career, "Sex" controversy, electronic club mix phase, and more.]]></abstract><clickurl>http://lrd.yahooapis.com/_ylc=X3oDMTQ4bDQyNGY3BF9TAzIwMjMxNTI3MDIEYXBwaWQDb0pfTWdwbklrWW5CMWhTZnFUZEd5TkouTXNxZlNMQmkEY2xpZW50A2Jvc3MEc2VydmljZQNCT1NTBHNsawN0aXRsZQRzcmNwdmlkA2pTRTJBMGdlQXUxVDcyems0VW5vdWczUTBZTS5jVW40NnNBQUFscVI-/SIG=11nj6b5l4/**http%3A//en.wikipedia.org/wiki/Madonna_(entertainer)</clickurl><date>2009/04/28</date><dispurl><![CDATA[<b>en.wikipedia.org</b>/wiki/<wbr><b>Madonna</b>_(entertainer)]]></dispurl><size>450316</size><title><![CDATA[<b>Madonna</b> (Entertainer) - Wikipedia]]></title><url>http://en.wikipedia.org/wiki/Madonna_(entertainer)</url></result><result xmlns="http://www.inktomi.com/"><abstract><![CDATA[<b>Madonna</b> MySpace page features news, blog, music downloads, desktops, wallpapers, and more.]]></abstract><clickurl>http://lrd.yahooapis.com/_ylc=X3oDMTQ4bDQyNGY3BF9TAzIwMjMxNTI3MDIEYXBwaWQDb0pfTWdwbklrWW5CMWhTZnFUZEd5TkouTXNxZlNMQmkEY2xpZW50A2Jvc3MEc2VydmljZQNCT1NTBHNsawN0aXRsZQRzcmNwdmlkA2pTRTJBMGdlQXUxVDcyems0VW5vdWczUTBZTS5jVW40NnNBQUFscVI-/SIG=113n192qg/**http%3A//www.myspace.com/madonna</clickurl><date>2009/04/29</date><dispurl><![CDATA[www.<b>myspace.com</b>/<b>madonna</b>]]></dispurl><size>110851</size><title><![CDATA[<b>Madonna</b> - MySpace]]></title><url>http://www.myspace.com/madonna</url></result><result xmlns="http://www.inktomi.com/"><abstract><![CDATA[The Official <b>Madonna</b> YouTube Channel. Want to Subscribe? ... http://www.youtube.com/<b>Madonna</b><wbr>. Sharing Options (close) There are 3 ways to share this channel.]]></abstract><clickurl>http://lrd.yahooapis.com/_ylc=X3oDMTQ4bDQyNGY3BF9TAzIwMjMxNTI3MDIEYXBwaWQDb0pfTWdwbklrWW5CMWhTZnFUZEd5TkouTXNxZlNMQmkEY2xpZW50A2Jvc3MEc2VydmljZQNCT1NTBHNsawN0aXRsZQRzcmNwdmlkA2pTRTJBMGdlQXUxVDcyems0VW5vdWczUTBZTS5jVW40NnNBQUFscVI-/SIG=10vv5undb/**http%3A//youtube.com/madonna</clickurl><date>2009/04/29</date><dispurl><![CDATA[<b>youtube.com</b>/<b>madonna</b>]]></dispurl><size>49698</size><title><![CDATA[YouTube - <b>madonna\'s</b> Channel]]></title><url>http://youtube.com/madonna</url></result><result xmlns="http://www.inktomi.com/"><abstract><![CDATA[4 Minutes - Behind the Scenes (Music Video) by <b>Madonna</b>. Give It 2 Me (Music ... first disable your pop-up blocker or add imeem.com to your pop-up "safe" list.]]></abstract><clickurl>http://lrd.yahooapis.com/_ylc=X3oDMTQ4bDQyNGY3BF9TAzIwMjMxNTI3MDIEYXBwaWQDb0pfTWdwbklrWW5CMWhTZnFUZEd5TkouTXNxZlNMQmkEY2xpZW50A2Jvc3MEc2VydmljZQNCT1NTBHNsawN0aXRsZQRzcmNwdmlkA2pTRTJBMGdlQXUxVDcyems0VW5vdWczUTBZTS5jVW40NnNBQUFscVI-/SIG=111t834o5/**http%3A//www.imeem.com/madonna</clickurl><date>2009/04/29</date><dispurl><![CDATA[www.<b>imeem.com</b>/<b>madonna</b>]]></dispurl><size>116486</size><title><![CDATA[<b>Madonna</b> Music Profile on IMEEM]]></title><url>http://www.imeem.com/madonna</url></result><result xmlns="http://www.inktomi.com/"><abstract><![CDATA[Stay current on the latest <b>Madonna</b> music videos, news, tour dates, ringtones and more on MTV - the leader in music news, video premieres and entertainment online.]]></abstract><clickurl>http://lrd.yahooapis.com/_ylc=X3oDMTQ4bDQyNGY3BF9TAzIwMjMxNTI3MDIEYXBwaWQDb0pfTWdwbklrWW5CMWhTZnFUZEd5TkouTXNxZlNMQmkEY2xpZW50A2Jvc3MEc2VydmljZQNCT1NTBHNsawN0aXRsZQRzcmNwdmlkA2pTRTJBMGdlQXUxVDcyems0VW5vdWczUTBZTS5jVW40NnNBQUFscVI-/SIG=1amlb9ane/**http%3A//rdrw1.yahoo.com/click%3Fu=http%3A//www.mtv.com/music/artist/madonna/artist.jhtml%26y=04EA3891AE02BA97B0%26i=1473%26c=50125%26mcid=C4F83E4CDAE5E571%26q=02%255ESSHPM%255BL7r~%257Bpqq~6%26e=utf-8%26r=5%26d=wow~oJ_MgpnIkYnB1hSfqTdGyNJ.MsqfSLBi-en-us%26n=98T47GOLSBS4IEQA%26s=12%26t=%26m=49F8EAA6%26x=051E6BA3BBD6F3CA8046DE93FCD3632CD0</clickurl><date>2009/04/27</date><dispurl><![CDATA[www.<b>mtv.com</b>/music/artist/<b>madonna</b>/<wbr>artist.jhtml]]></dispurl><size>35769</size><title><![CDATA[<b>Madonna</b> | Music Artist | Videos, News, Photos &amp; Ringtones | MTV]]></title><url>http://www.mtv.com/music/artist/madonna/artist.jhtml</url></result><result xmlns="http://www.inktomi.com/"><abstract><![CDATA[Pictures, articles, downloads, concert info, news, and more about <b>Madonna</b>.]]></abstract><clickurl>http://lrd.yahooapis.com/_ylc=X3oDMTQ4bDQyNGY3BF9TAzIwMjMxNTI3MDIEYXBwaWQDb0pfTWdwbklrWW5CMWhTZnFUZEd5TkouTXNxZlNMQmkEY2xpZW50A2Jvc3MEc2VydmljZQNCT1NTBHNsawN0aXRsZQRzcmNwdmlkA2pTRTJBMGdlQXUxVDcyems0VW5vdWczUTBZTS5jVW40NnNBQUFscVI-/SIG=113uc4v71/**http%3A//www.madonnalicious.com/</clickurl><date>2009/01/05</date><dispurl><![CDATA[www.<b>madonnalicious.com</b>]]></dispurl><size>969</size><title>Madonnalicious</title><url>http://www.madonnalicious.com/</url></result><result xmlns="http://www.inktomi.com/"><abstract><![CDATA[Find <b>Madonna</b> Songs, Photos, Music Videos, Artist News, Album Info and More on MSN Music.]]></abstract><clickurl>http://lrd.yahooapis.com/_ylc=X3oDMTQ4bDQyNGY3BF9TAzIwMjMxNTI3MDIEYXBwaWQDb0pfTWdwbklrWW5CMWhTZnFUZEd5TkouTXNxZlNMQmkEY2xpZW50A2Jvc3MEc2VydmljZQNCT1NTBHNsawN0aXRsZQRzcmNwdmlkA2pTRTJBMGdlQXUxVDcyems0VW5vdWczUTBZTS5jVW40NnNBQUFscVI-/SIG=1bgmfofdr/**http%3A//rdrw1.yahoo.com/click%3Fu=http%3A//inkt.trafficdashboard.com/track.htm%253Fpid%253D16221546%2526kwid%253D35172811%2526cid%253D2354%26y=04CC4C749CDBC8C51B%26i=1473%26c=49372%26q=02%255ESSHPM%255BL7r~%257Bpqq~6%26e=utf-8%26r=7%26d=wow~oJ_MgpnIkYnB1hSfqTdGyNJ.MsqfSLBi-en-us%26n=98T47GOLSBS4IEQA%26s=12%26t=%26m=49F8EAA6%26x=05C95D3A6752E11CF37757D1BD876F1B9D</clickurl><date>2009/04/12</date><dispurl><![CDATA[<b>music.msn.com</b>/artist/<wbr>?artist=16119704]]></dispurl><size>0</size><title><![CDATA[<b>Madonna</b> on MSN Music]]></title><url>http://music.msn.com/artist/?artist=16119704</url></result><result xmlns="http://www.inktomi.com/"><abstract><![CDATA[Fan site featuring bio, latest news, picture gallery, audio-video downloads, and forum for <b>Madonna</b>.]]></abstract><clickurl>http://lrd.yahooapis.com/_ylc=X3oDMTQ4bDQyNGY3BF9TAzIwMjMxNTI3MDIEYXBwaWQDb0pfTWdwbklrWW5CMWhTZnFUZEd5TkouTXNxZlNMQmkEY2xpZW50A2Jvc3MEc2VydmljZQNCT1NTBHNsawN0aXRsZQRzcmNwdmlkA2pTRTJBMGdlQXUxVDcyems0VW5vdWczUTBZTS5jVW40NnNBQUFscVI-/SIG=1143vgj6o/**http%3A//www.allaboutmadonna.com/</clickurl><date>2009/04/28</date><dispurl><![CDATA[www.<b>allaboutmadonna.com</b>]]></dispurl><size>68738</size><title><![CDATA[All About <b>Madonna</b>]]></title><url>http://www.allaboutmadonna.com/</url></result><result xmlns="http://www.inktomi.com/"><abstract>Catholic facility devoted entirely to physical rehabilitation, serving all faiths.</abstract><clickurl>http://lrd.yahooapis.com/_ylc=X3oDMTQ4bDQyNGY3BF9TAzIwMjMxNTI3MDIEYXBwaWQDb0pfTWdwbklrWW5CMWhTZnFUZEd5TkouTXNxZlNMQmkEY2xpZW50A2Jvc3MEc2VydmljZQNCT1NTBHNsawN0aXRsZQRzcmNwdmlkA2pTRTJBMGdlQXUxVDcyems0VW5vdWczUTBZTS5jVW40NnNBQUFscVI-/SIG=10s16mpes/**http%3A//www.madonna.org/</clickurl><date>2009/04/28</date><dispurl><![CDATA[www.<b>madonna.org</b>]]></dispurl><size>12062</size><title><![CDATA[<b>Madonna</b> Rehabilitation Hospital]]></title><url>http://www.madonna.org/</url></result></results></query>');

// There's a bunch of data above.  Maybe you should scroll down from the
// top instead.


}, '@VERSION@' ,{requires:['datasource-xmlschema', 'test', 'datatype-xml-parse']});
