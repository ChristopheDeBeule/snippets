String resizeString(String str, int length) {
    // Ensure the length does not exceed the max allowed length
    int maxLength = 32767 // Jira max allowed length
    length = Math.min(length, maxLength)
     println(length)
    // Return string as is if it's already shorter than or equal to the desired length
    if (str.size() <= length) return str

    // Extract the first 'length' characters
    String returnStr = str[0..length - 1]

    // If the string exceeds the length and the cut-off character is not a space
    if (str[length] != ' ' && returnStr.lastIndexOf(' ') != -1) {
        // Find the last space in the truncated string
        int lastSpaceIndex = returnStr.lastIndexOf(' ')

        // If space exists, truncate at the last space to avoid cutting off a word
        returnStr = returnStr[0..lastSpaceIndex - 1]
    }
    return returnStr
}


// Example

String loremIpsum1 = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Praesent efficitur, orci non sodales facilisis, elit dolor scelerisque justo, a dictum mi quam id libero. Fusce sed laoreet enim." // n characters
String loremIpsum2 = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Sed do." // < 80 characters

println(resizeString(loremIpsum1, 90)) // Will be stripped to 90 characters or less.
println(resizeString(loremIpsum2, 90)) // Will still be < 80 characters.
