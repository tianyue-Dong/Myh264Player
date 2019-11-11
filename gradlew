/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.ex.chips;

import android.annotation.TargetApi;
import android.content.ClipData;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.SmallTest;
import android.text.Editable;
import android.text.SpannableStringBuilder;
import android.text.util.Rfc822Tokenizer;
import android.widget.TextView;

import com.android.ex.chips.recipientchip.DrawableRecipientChip;
import com.android.ex.chips.recipientchip.ReplacementDrawableSpan;
import com.android.ex.chips.recipientchip.VisibleRecipientChip;

import java.util.regex.Pattern;

@SmallTest
public class ChipsTest extends AndroidTestCase {
    private DrawableRecipientChip[] mMockRecips;

    private RecipientEntry[] mMockEntries;

    private Rfc822Tokenizer mTokenizer;

    private Editable mEditable;

    class BaseMockRecipientEditTextView extends RecipientEditTextView {

        public BaseMockRecipientEditTextView(Context context) {
            super(context, null);
            mTokenizer = new Rfc822Tokenizer();
            setTokenizer(mTokenizer);
        }

        @Override
        public DrawableRecipientChip[] getSortedRecipients() {
            return mMockRecips;
        }

        @Override
        public int getLineHeight() {
            return 48;
        }

        @Override
        Drawable getChipBackground(RecipientEntry contact) {
            return createChipBackground();
        }

        @Override
        public int getViewWidth() {
            return 100;
        }
    }

    class MockRecipientEditTextView extends BaseMockRecipientEditTextView {

        public MockRecipientEditTextView(Context context) {
            super(context);
            mTokenizer = new Rfc822Tokenizer();
            setTokenizer(mTokenizer);
        }

        @Override
        public DrawableRecipientChip[] getSortedRecipients() {
            return mMockRecips;
        }

        @Override
        public Editable getText() {
            return mEditable;
        }

        @Override
        public Editable getSpannable() {
            return mEditable;
        }

        @Override
        public int getLineHeight() {
            return 48;
        }

        @Override
        Drawable getChipBackground(RecipientEntry contact) {
            return createChipBackground();
        }

        @Override
        public int length() {
            return mEditable != null ? mEditable.length() : 0;
        }

        @Override
        public String toString() {
            return mEditable != null ? mEditable.toString() : "";
        }

        @Override
        public int getViewWidth() {
            return 100;
        }
    }

    private class TestBaseRecipientAdapter extends BaseRecipientAdapter {
        public TestBaseRecipientAdapter(final Context context) {
            super(context);
        }

        public TestBaseRecipientAdapter(final Context context, final int preferredMaxResultCount,
                final int queryMode) {
            super(context, preferredMaxResultCount, queryMode);
        }
    }

    private MockRecipientEditTextView createViewForTesting() {
        mEditable = new SpannableStringBuilder();
        MockRecipientEditTextView view = new MockRecipientEditTextView(getContext());
        view.setAdapter(new TestBaseRecipientAdapter(getContext()));
        return view;
    }

    public void testCreateDisplayText() {
        RecipientEditTextView view = createViewForTesting();
        RecipientEntry entry = RecipientEntry.constructGeneratedEntry("User Name, Jr",
                "user@username.com", true);
        String testAddress = view.createAddressText(entry);
        String testDisplay = view.createChipDisplayText(entry);
        assertEquals("Expected a properly formatted RFC email address",
                "\"User Name, Jr\" <user@username.com>, ", testAddress);
        assertEquals("Expected a displayable name", "User Name, Jr", testDisplay);

        RecipientEntry alreadyFormatted =
                RecipientEntry.constructFakeEntry("user@username.com, ", true);
        testAddress = view.createAddressText(alreadyFormatted);
        testDisplay = view.createChipDisplayText(alreadyFormatted);
        assertEquals("Expected a properly formatted RFC email address", "<user@username.com>, ",
                testAddress);
        assertEquals("Expected a displayable name", "user@username.com", testDisplay);

        RecipientEntry alreadyFormattedNoSpace = RecipientEntry
                .constru