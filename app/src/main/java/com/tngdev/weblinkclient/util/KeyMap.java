/****************************************************************************
 *
 * @file KeyMap.java
 * @brief
 *
 * Contains the KeyMap class.
 *
 * @author Abalta Technologies, Inc.
 * @date Jan, 2014
 *
 * @cond Copyright
 *
 * COPYRIGHT 2014 ABALTA TECHNOLOGIES ALL RIGHTS RESERVED.<br>
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
 *
 * @endcond
 *****************************************************************************/
package com.tngdev.weblinkclient.util;

import android.util.SparseIntArray;
import android.view.KeyEvent;

/**
 * KeyMap bridges android key codes and the default ASCII values.  Most should be identical,
 * but this is just in case.
 */
public class KeyMap {

    public static final SparseIntArray KEY_MAPPINGS_NORMAL;
    public static final SparseIntArray KEY_MAPPINGS_SHIFT;

	 static {
	        SparseIntArray keyMapping = new SparseIntArray();
	        
	        keyMapping.put(KeyEvent.KEYCODE_Q, (int)'q');
	        keyMapping.put(KeyEvent.KEYCODE_W, (int)'w');
	        keyMapping.put(KeyEvent.KEYCODE_E, (int)'e');
	        keyMapping.put(KeyEvent.KEYCODE_R, (int)'r');
	        keyMapping.put(KeyEvent.KEYCODE_T, (int)'t');
	        keyMapping.put(KeyEvent.KEYCODE_Y, (int)'y');
	        keyMapping.put(KeyEvent.KEYCODE_U, (int)'u');
	        keyMapping.put(KeyEvent.KEYCODE_I, (int)'i');
	        keyMapping.put(KeyEvent.KEYCODE_O, (int)'o');
	        keyMapping.put(KeyEvent.KEYCODE_P, (int)'p');
	        keyMapping.put(KeyEvent.KEYCODE_A, (int)'a');
	        keyMapping.put(KeyEvent.KEYCODE_S, (int)'s');
	        keyMapping.put(KeyEvent.KEYCODE_D, (int)'d');
	        keyMapping.put(KeyEvent.KEYCODE_F, (int)'f');
	        keyMapping.put(KeyEvent.KEYCODE_G, (int)'g');
	        keyMapping.put(KeyEvent.KEYCODE_H, (int)'h');
	        keyMapping.put(KeyEvent.KEYCODE_J, (int)'j');
	        keyMapping.put(KeyEvent.KEYCODE_K, (int)'k');
	        keyMapping.put(KeyEvent.KEYCODE_L, (int)'l');
	        keyMapping.put(KeyEvent.KEYCODE_Z, (int)'z');
	        keyMapping.put(KeyEvent.KEYCODE_X, (int)'x');
	        keyMapping.put(KeyEvent.KEYCODE_C, (int)'c');
	        keyMapping.put(KeyEvent.KEYCODE_V, (int)'v');
	        keyMapping.put(KeyEvent.KEYCODE_B, (int)'b');
	        keyMapping.put(KeyEvent.KEYCODE_N, (int)'n');
	        keyMapping.put(KeyEvent.KEYCODE_M, (int)'m');
	        //--
	        keyMapping.put(KeyEvent.KEYCODE_0, (int)'0');
	        keyMapping.put(KeyEvent.KEYCODE_1, (int)'1');
	        keyMapping.put(KeyEvent.KEYCODE_2, (int)'2');
	        keyMapping.put(KeyEvent.KEYCODE_3, (int)'3');
	        keyMapping.put(KeyEvent.KEYCODE_4, (int)'4');
	        keyMapping.put(KeyEvent.KEYCODE_5, (int)'5');
	        keyMapping.put(KeyEvent.KEYCODE_6, (int)'6');
	        keyMapping.put(KeyEvent.KEYCODE_7, (int)'7');
	        keyMapping.put(KeyEvent.KEYCODE_8, (int)'8');
	        keyMapping.put(KeyEvent.KEYCODE_9, (int)'9');
	        //--
            keyMapping.put(KeyEvent.KEYCODE_GRAVE, (int)'`');
            keyMapping.put(KeyEvent.KEYCODE_EQUALS, (int)'=');
            keyMapping.put(KeyEvent.KEYCODE_LEFT_BRACKET, (int)'[');
            keyMapping.put(KeyEvent.KEYCODE_RIGHT_BRACKET, (int)']');
            //--
	       
	        keyMapping.put(KeyEvent.KEYCODE_DEL,   8);   // Del 
	        keyMapping.put(KeyEvent.KEYCODE_ENTER, 10); // Enter
	        keyMapping.put(KeyEvent.KEYCODE_SPACE, 32); // Space
	        //--        
	        keyMapping.put(KeyEvent.KEYCODE_NUMPAD_DOT,         (int)'.');
	        keyMapping.put(KeyEvent.KEYCODE_PERIOD,         	(int)'.');
	        keyMapping.put(KeyEvent.KEYCODE_COMMA,              (int)',');  
	        keyMapping.put(KeyEvent.KEYCODE_AT,                 (int)'@');  
	        keyMapping.put(KeyEvent.KEYCODE_POUND,              (int)'#');  
	        keyMapping.put(KeyEvent.KEYCODE_NUMPAD_MULTIPLY,    (int)'*');  
	        keyMapping.put(KeyEvent.KEYCODE_NUMPAD_LEFT_PAREN,  (int)'(');  
	        keyMapping.put(KeyEvent.KEYCODE_NUMPAD_RIGHT_PAREN, (int)')');  
	        keyMapping.put(KeyEvent.KEYCODE_MINUS,              (int)'-');
	        keyMapping.put(KeyEvent.KEYCODE_PLUS,               (int)'+');  
	        keyMapping.put(KeyEvent.KEYCODE_NUMPAD_EQUALS,      (int)'=');  
	        keyMapping.put(KeyEvent.KEYCODE_LEFT_BRACKET,       (int)'[');  
	        keyMapping.put(KeyEvent.KEYCODE_RIGHT_BRACKET,      (int)']');  
	        keyMapping.put(KeyEvent.KEYCODE_BACKSLASH,          (int)'\\'); 
	        keyMapping.put(KeyEvent.KEYCODE_NUMPAD_DIVIDE,      (int)'/'); 
	        keyMapping.put(KeyEvent.KEYCODE_SEMICOLON,          (int)';'); 
	        keyMapping.put(KeyEvent.KEYCODE_APOSTROPHE,         (int)'\'');
	        keyMapping.put(KeyEvent.KEYCODE_SLASH,              (int)'/');    
	        //--
	        
	        SparseIntArray keyMappingShift = new SparseIntArray();
	        
	        keyMappingShift.put(KeyEvent.KEYCODE_Q, (int)'Q');
	        keyMappingShift.put(KeyEvent.KEYCODE_W, (int)'W');
	        keyMappingShift.put(KeyEvent.KEYCODE_E, (int)'E');
	        keyMappingShift.put(KeyEvent.KEYCODE_R, (int)'R');
	        keyMappingShift.put(KeyEvent.KEYCODE_T, (int)'T');
	        keyMappingShift.put(KeyEvent.KEYCODE_Y, (int)'Y');
	        keyMappingShift.put(KeyEvent.KEYCODE_U, (int)'U');
	        keyMappingShift.put(KeyEvent.KEYCODE_I, (int)'I');
	        keyMappingShift.put(KeyEvent.KEYCODE_O, (int)'O');
	        keyMappingShift.put(KeyEvent.KEYCODE_P, (int)'P');
	        keyMappingShift.put(KeyEvent.KEYCODE_A, (int)'A');
	        keyMappingShift.put(KeyEvent.KEYCODE_S, (int)'S');
	        keyMappingShift.put(KeyEvent.KEYCODE_D, (int)'D');
	        keyMappingShift.put(KeyEvent.KEYCODE_F, (int)'F');
	        keyMappingShift.put(KeyEvent.KEYCODE_G, (int)'G');
	        keyMappingShift.put(KeyEvent.KEYCODE_H, (int)'H');
	        keyMappingShift.put(KeyEvent.KEYCODE_J, (int)'J');
	        keyMappingShift.put(KeyEvent.KEYCODE_K, (int)'K');
	        keyMappingShift.put(KeyEvent.KEYCODE_L, (int)'L');
	        keyMappingShift.put(KeyEvent.KEYCODE_Z, (int)'Z');
	        keyMappingShift.put(KeyEvent.KEYCODE_X, (int)'X');
	        keyMappingShift.put(KeyEvent.KEYCODE_C, (int)'C');
	        keyMappingShift.put(KeyEvent.KEYCODE_V, (int)'V');
	        keyMappingShift.put(KeyEvent.KEYCODE_B, (int)'B');
	        keyMappingShift.put(KeyEvent.KEYCODE_N, (int)'N');
	        keyMappingShift.put(KeyEvent.KEYCODE_M, (int)'M');
	        //--
	        keyMappingShift.put(KeyEvent.KEYCODE_0, (int)')');
	        keyMappingShift.put(KeyEvent.KEYCODE_1, (int)'!');
	        keyMappingShift.put(KeyEvent.KEYCODE_2, (int)'@');
	        keyMappingShift.put(KeyEvent.KEYCODE_3, (int)'#');
	        keyMappingShift.put(KeyEvent.KEYCODE_4, (int)'$');
	        keyMappingShift.put(KeyEvent.KEYCODE_5, (int)'%');
	        keyMappingShift.put(KeyEvent.KEYCODE_6, (int)'^');
	        keyMappingShift.put(KeyEvent.KEYCODE_7, (int)'&');
	        keyMappingShift.put(KeyEvent.KEYCODE_8, (int)'*');
	        keyMappingShift.put(KeyEvent.KEYCODE_9, (int)'(');
	        
	        //--
	        keyMappingShift.put(KeyEvent.KEYCODE_SEMICOLON,  (int)':');
	        keyMappingShift.put(KeyEvent.KEYCODE_APOSTROPHE, (int)'\"');
	        keyMappingShift.put(KeyEvent.KEYCODE_SLASH,      (int)'?');
	        keyMappingShift.put(KeyEvent.KEYCODE_MINUS,      (int)'_'); // Minus is underscore in Shift mode
	        
	        //--

            keyMappingShift.put(KeyEvent.KEYCODE_GRAVE, (int)'~');
            keyMappingShift.put(KeyEvent.KEYCODE_BACKSLASH, (int)'|');
            keyMappingShift.put(KeyEvent.KEYCODE_LEFT_BRACKET, (int)'{');
            keyMappingShift.put(KeyEvent.KEYCODE_RIGHT_BRACKET, (int)'}');
            //--
	        
	        KEY_MAPPINGS_NORMAL = keyMapping;
	        KEY_MAPPINGS_SHIFT  = keyMappingShift; 
	 }
}
