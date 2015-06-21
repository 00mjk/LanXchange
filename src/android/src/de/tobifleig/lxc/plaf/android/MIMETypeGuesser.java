/*
 * Copyright 2009, 2010, 2011, 2012, 2013, 2014, 2015 Tobias Fleig (tobifleig gmail com)
 *
 * All rights reserved.
 *
 * This file is part of LanXchange.
 *
 * LanXchange is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LanXchange is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LanXchange. If not, see <http://www.gnu.org/licenses/>.
 */
package de.tobifleig.lxc.plaf.android;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.provider.MediaStore;
import android.webkit.MimeTypeMap;
import de.tobifleig.lxc.data.impl.RealFile;

import java.io.IOException;
import java.net.URLConnection;

/**
 * Tries to figure out the MIMEType of received files, so they can be opened.
 */
public class MIMETypeGuesser {

    private final static String GENERIC_RESULT = "application/octet-stream";

    private MIMETypeGuesser() {
    }

    /**
     * Try to guess the MIMEType for the given file.
     * This method tries up to three (increasingly hacky) ways to get the MIMEType.
     * The first meaningful result is returned.
     * These methods are:
     * 1. Believe the extension (if any) and ask MimeTypeMap
     * 2. Look at magic numbers via URLConnection.guessContentTypeFromStream
     * 3. Ask the ContentResolver
     * 4. Ask the MediaStore
     * If everything fails, the result is "application/octet-stream"
     *
     * @param file the file to guess the type from
     * @param context the context, required for some guessing methods
     * @return a valid MIMEType, "application/octet-stream" if everything fails
     */
    public static String guessMIMEType(RealFile file, Context context) {
        Uri fileUri = Uri.fromFile(file.getBackingFile());
        String name = file.getName();
        // hack #0, MimeTypeMap.getFileExtensionFromUrl fails if the name contains '#'
        String extension = MimeTypeMap.getFileExtensionFromUrl(file.getBackingFile().getAbsolutePath().replaceAll("#", ""));

        // METHOD 1: Use file extension
        if (extension != null && !extension.isEmpty()) {
            String result = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
            if (isUsefulMIMEType(result)) {
                return result;
            }
        }

        // METHOD 2: Look at magic number
        try {
            String magicResult = URLConnection.guessContentTypeFromStream(file.getInputStream());
            if (isUsefulMIMEType(magicResult)) {
                return magicResult;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        // METHOD 3: Ask the content resolver
        String contentResolverResult = context.getContentResolver().getType(fileUri);
        if (isUsefulMIMEType(contentResolverResult)) {
            return contentResolverResult;
        }

        // Everything failed
        System.err.println("Cannot guess MIMEType for file " + file.getBackingFile().getAbsolutePath());
        return GENERIC_RESULT;
    }

    /**
     * Helper, test that the result is neither null, empty nor GENERIC_RESULT
     *
     * @param MIMEType the guessed type to check
     * @return true if (probably) useful
     */
    private static boolean isUsefulMIMEType(String MIMEType) {
        return MIMEType != null && !MIMEType.isEmpty() && !MIMEType.equals(GENERIC_RESULT);
    }
}
