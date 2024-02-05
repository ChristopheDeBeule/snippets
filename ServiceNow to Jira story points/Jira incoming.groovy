def toFloat(String field){
    field = field.replaceAll(",", "")
    return Float.parseFloat(field)
}

issue.customFields."Story Points".value = replica.story_points
issue.customFields."Story point estimate".value = toFloat(replica.story_points)