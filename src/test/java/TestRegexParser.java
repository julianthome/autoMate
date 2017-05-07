/*
* automat - bdd automaton package
*
* Copyright 2016, Julian Thomé <julian.thome.de@gmail.com>
*
* Licensed under the EUPL, Version 1.1 or – as soon they will be approved by
* the European Commission - subsequent versions of the EUPL (the "Licence");
* You may not use this work except in compliance with the Licence. You may
* obtain a copy of the Licence at:
*
* https://joinup.ec.europa.eu/sites/default/files/eupl1.1.-licence-en_0.pdf
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the Licence is distributed on an "AS IS" basis, WITHOUT
* WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the Licence for the specific language governing permissions and
* limitations under the Licence.
*/

import com.github.julianthome.automate.core.Automaton;
import com.github.julianthome.automate.parser.RegexParser;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class TestRegexParser {

    final static Logger LOGGER = LoggerFactory.getLogger(TestRegexParser.class);


    @Test
    public void testPattern0() {

        Automaton a = RegexParser.INSTANCE.getAutomaton("abc*[a-z]?d");



        LOGGER.debug(a.toDot());



    }

}